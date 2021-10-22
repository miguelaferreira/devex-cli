package devex.terraform;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.vavr.collection.Stream;
import io.vavr.control.Either;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.assertj.vavr.api.VavrAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@MicronautTest
class TerraformStateServiceTest {

    private static final TerraformModuleDirectory DIRECTORY = new TerraformModuleDirectory(Path.of("src/test/resources/terraform/module1"));

    @Inject
    private TerraformStateService terraformStateService;

    // service that performs apply and destroy for testing
    private static final TerraformTestService terraformStateServiceTest = new TerraformTestService();

    @BeforeAll
    static void beforeAll() {
        final Either<String, String> maybeInitOutput = terraformStateServiceTest.terraformInit(DIRECTORY);
        log.debug("==> Terraform Init output:\n" + maybeInitOutput.get());
        final Either<String, String> maybeApplyOutput = terraformStateServiceTest.terraformApply(DIRECTORY);
        log.debug("==> Terraform apply output:\n" + maybeApplyOutput.get());
    }

    @AfterAll
    static void afterAll() {
        final Either<String, String> maybeOutput = terraformStateServiceTest.terraformDestroy(DIRECTORY);
        log.debug("==> Terraform destroy output:\n" + maybeOutput.get());
    }

    @Test
    void pullStateFile_success() {
        final Either<String, TerraformStateFile> maybeState = terraformStateService.pullStateFile(DIRECTORY);

        VavrAssertions.assertThat(maybeState)
                      .hasRightValueSatisfying(
                              terraformStateFile -> assertThat(terraformStateFile).isNotNull()
                                                                                  .extracting("stateFile")
                                                                                  .as("stateFile")
                                                                                  .isNotNull()
                      );
    }

    @Test
    void pullStateFileAsync_success() {
        final Either<String, TerraformStateFile> maybeState = terraformStateService.pullStateFileAsync(DIRECTORY)
                                                                                   .blockFirst();

        VavrAssertions.assertThat(maybeState)
                      .hasRightValueSatisfying(
                              terraformStateFile -> assertThat(terraformStateFile).isNotNull()
                                                                                  .extracting("stateFile")
                                                                                  .as("stateFile")
                                                                                  .isNotNull()
                      );
    }

    @Test
    void pullState_noModule() {
        final TerraformModuleDirectory directory = new TerraformModuleDirectory(Path.of("src/test/resources/terraform/does-not-exist"));
        final Either<String, TerraformStateFile> maybeState = terraformStateService.pullStateFile(directory);

        VavrAssertions.assertThat(maybeState)
                      .hasLeftValueSatisfying(
                              errorMessage -> assertThat(errorMessage).isNotNull()
                                                                      .endsWith("doesn't exist.")
                      );
    }

    @Test
    void listSecrets_success() {
        final Either<String, Stream<String>> maybeSecretsList = terraformStateService.listSecrets(DIRECTORY);

        VavrAssertions.assertThat(maybeSecretsList)
                      .hasRightValueSatisfying(
                              secretsList -> VavrAssertions.assertThat(secretsList)
                                                           .isNotEmpty()
                                                           .hasSize(2)
                      );
    }

    @Test
    void listSecretsAsync_success() {
        final Either<String, Stream<String>> maybeSecretsList = terraformStateService.listSecretsAsync(DIRECTORY)
                                                                                     .blockFirst();

        VavrAssertions.assertThat(maybeSecretsList)
                      .hasRightValueSatisfying(
                              secretsList -> VavrAssertions.assertThat(secretsList)
                                                           .isNotEmpty()
                                                           .hasSize(2)
                      );
    }

    @Test
    void taintSecrets_success() {
        final Either<String, Stream<Either<String, String>>> maybeSecretsList = terraformStateService.taintSecrets(DIRECTORY);

        VavrAssertions.assertThat(maybeSecretsList)
                      .hasRightValueSatisfying(
                              secretsList -> VavrAssertions.assertThat(secretsList)
                                                           .isNotEmpty()
                                                           .hasSize(2)
                      );
    }

    @Test
    void taintSecretsAsync_success() {
        final Either<String, Flux<Either<String, String>>> maybeSecretsList = terraformStateService.taintSecretsAsync(DIRECTORY)
                                                                                                   .blockFirst();

        VavrAssertions.assertThat(maybeSecretsList)
                      .hasRightValueSatisfying(
                              secretsList -> assertThat(secretsList.collectList().block())
                                      .isNotEmpty()
                                      .hasSize(2)
                      );
    }
}
