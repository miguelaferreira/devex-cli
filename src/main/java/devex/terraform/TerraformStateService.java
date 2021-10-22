package devex.terraform;

import io.micronaut.context.annotation.Value;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Either;
import io.vavr.control.Try;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.function.Function;

@Slf4j
@Singleton
public class TerraformStateService {

    private final TerraformCommand command;
    private final List<String> resourcesWithSecrets;

    public TerraformStateService(TerraformCommand command, @Value("${terraform.secrets}") final java.util.List<String> resourcesWithSecrets) {
        this.command = command;
        this.resourcesWithSecrets = List.ofAll(resourcesWithSecrets);
    }

    public Either<String, TerraformStateFile> pullStateFile(TerraformModuleDirectory directory) {
        return createTemporaryStateFile(command.execute(directory, new String[]{"state", "pull"}));
    }

    public Flux<Either<String, TerraformStateFile>> pullStateFileAsync(TerraformModuleDirectory directory) {
        return command.executeAsync(directory, new String[]{"state", "pull"})
                      .map(this::createTemporaryStateFile);
    }

    private Either<String, TerraformStateFile> createTemporaryStateFile(final Either<String, String> maybeStateContent) {
        return maybeStateContent.flatMap(this::buildStateFileObject);
    }

    private Either<String, TerraformStateFile> buildStateFileObject(final String content) {
        return Try.of(() -> new TerraformStateFile(content))
                  .toEither()
                  .mapLeft(t -> CommandUtils.handleOperationException(t, "create terraform state file", log));
    }

    public Either<String, Stream<String>> listSecrets(TerraformModuleDirectory directory) {
        final Either<String, String> maybeStateList = command.execute(directory, new String[]{"state", "list"});
        return filterSecrets(maybeStateList);
    }

    public Flux<Either<String, Stream<String>>> listSecretsAsync(TerraformModuleDirectory directory) {
        return command.executeAsync(directory, new String[]{"state", "list"})
                      .map(this::filterSecrets);
    }

    private Either<String, Stream<String>> filterSecrets(final Either<String, String> maybeStateList) {
        return maybeStateList.map(this::parseStateList)
                             .map(stateList -> stateList.filter(this::isSecret));
    }

    private boolean isSecret(final String resource) {
        return !Stream.ofAll(Arrays.stream(resource.split("\\.")))
                      .filter(resourcesWithSecrets::contains)
                      .isEmpty();
    }

    private Stream<String> parseStateList(final String stateListString) {
        log.trace("stateList = \n{}", stateListString);
        return Stream.ofAll(Arrays.stream(stateListString.split("\n")));
    }

    public Either<String, Stream<Either<String, String>>> taintSecrets(TerraformModuleDirectory directory) {
        return taintSecrets(directory, false);
    }

    public Either<String, Stream<Either<String, String>>> taintSecrets(TerraformModuleDirectory directory, final boolean untaint) {
        return taintResources(directory, this.listSecrets(directory), untaint);
    }

    public Flux<Either<String, Flux<Either<String, String>>>> taintSecretsAsync(TerraformModuleDirectory directory) {
        return taintSecretsAsync(directory, false);
    }

    public Flux<Either<String, Flux<Either<String, String>>>> taintSecretsAsync(TerraformModuleDirectory directory, final boolean untaint) {
        return this.listSecretsAsync(directory)
                   .map(maybeList -> taintResourcesAsync(directory, maybeList, untaint));
    }

    private Either<String, Stream<Either<String, String>>> taintResources(final TerraformModuleDirectory directory, final Either<String, Stream<String>> maybeList,
                                                                          final boolean untaint) {
        return maybeList.map(resourcesWithSecrets -> resourcesWithSecrets.map(resource -> command.execute(directory, taintCommand(resource, untaint))));
    }

    private Either<String, Flux<Either<String, String>>> taintResourcesAsync(final TerraformModuleDirectory directory, final Either<String, Stream<String>> maybeList,
                                                                             final boolean untaint) {
        return maybeList.map(resourcesWithSecrets ->
                Flux.fromIterable(resourcesWithSecrets.map(resource -> command.executeAsync(directory, taintCommand(resource, untaint))))
                    .flatMap(Function.identity())
        );
    }

    private String[] taintCommand(final String resource, final boolean untaint) {
        return new String[]{untaint ? "untaint" : "taint", resource};
    }
}
