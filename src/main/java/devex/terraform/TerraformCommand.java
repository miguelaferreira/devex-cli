package devex.terraform;

import io.micronaut.context.annotation.Value;
import io.vavr.control.Either;
import jakarta.inject.Singleton;
import reactor.core.publisher.Flux;

@Singleton
public class TerraformCommand {
    private final String executablePath;

    public TerraformCommand(@Value("${terraform.command}") String executablePath) {
        this.executablePath = executablePath;
    }

    public Either<String, String> execute(TerraformModuleDirectory directory, String[] arguments) {
        return directory.execute(executablePath, arguments);
    }

    public Flux<Either<String, String>> executeAsync(TerraformModuleDirectory directory, String[] arguments) {
        return directory.executeAsync(executablePath, arguments);
    }
}
