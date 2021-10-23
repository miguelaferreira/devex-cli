package devex;

import ch.qos.logback.core.joran.spi.JoranException;
import devex.terraform.TerraformModuleDirectory;
import devex.terraform.TerraformTestService;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import io.vavr.control.Either;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class TerraformTaintSecretsCommandTest {

    public static final String TEST_MODULE_PATH = "src/test/resources/terraform/module1";
    private static final TerraformModuleDirectory DIRECTORY = new TerraformModuleDirectory(Path.of(TEST_MODULE_PATH));

    // service that performs apply and destroy for testing
    private static TerraformTestService terraformStateServiceTest = new TerraformTestService();

    @BeforeAll
    static void beforeAll() throws JoranException {
        // Need to do this because configuration is static and loaded once per JVM,
        // if the full logs test runs before this one the full log configuration
        // remains active.
        LoggingConfiguration.loadLogsConfig();

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
    public void run_taint_secrets_verbose() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            String[] args = new String[]{"terraform", "taint-secrets", "-v", TEST_MODULE_PATH};
            DevexCommand.execute(ctx, args);

            assertThat(baos.toString()).contains("Set application loggers to DEBUG")
                                       .contains("Tainting terraform resources in module '" + TEST_MODULE_PATH + "'")
                                       .contains("Taint: Resource instance random_string.some_string has been marked as tainted.")
                                       .contains("Taint: Resource instance module.sub_module.random_string.some_other_string has been marked as tainted.")
                                       .contains("To rotate the secrets run terraform apply on the module.")
                                       .contains("All done");
        }
    }

    @Test
    public void run_untaint_secrets() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            String[] args = new String[]{"terraform", "taint-secrets", TEST_MODULE_PATH};
            DevexCommand.execute(ctx, args);

            args = new String[]{"terraform", "taint-secrets", "-u", TEST_MODULE_PATH};
            DevexCommand.execute(ctx, args);

            assertThat(baos.toString()).contains("Untainting terraform resources in module '" + TEST_MODULE_PATH + "'")
                                       .contains("Untaint: Resource instance random_string.some_string has been successfully untainted.")
                                       .contains("Untaint: Resource instance module.sub_module.random_string.some_other_string has been successfully untainted.")
                                       .contains("To rotate the secrets run terraform apply on the module.")
                                       .contains("All done");
        }
    }


}
