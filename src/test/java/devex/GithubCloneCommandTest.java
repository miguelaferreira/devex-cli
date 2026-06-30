package devex;

import ch.qos.logback.core.joran.spi.JoranException;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ssh-integration")
@EnabledIfEnvironmentVariable(named = "DEVEX_SSH_INTEGRATION_TESTS", matches = "true",
        disabledReason = "Clone tests hit the network; set DEVEX_SSH_INTEGRATION_TESTS=true to run.")
public class GithubCloneCommandTest extends TestBase {

    private static final String USER = "octocat";
    private static final int TEST_OUTPUT_ARRAY_SIZE = 4096000;

    @TempDir
    File cloneDirectory;

    @BeforeAll
    static void beforeAll() throws JoranException {
        // Configuration is static and loaded once per JVM; reset it so a full-logs
        // test running earlier does not leave the full log configuration active.
        LoggingConfiguration.loadLogsConfig();
    }

    @Test
    public void run_userName_clonesUserRepositoriesAsOrganization() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(TEST_OUTPUT_ARRAY_SIZE);
        System.setOut(new PrintStream(baos));

        try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
            String[] args = new String[]{"github", "clone", "-v", "-c", "HTTPS", USER, cloneDirectory.toPath().toString()};
            DevexCommand.execute(ctx, args);

            final String output = baos.toString();
            assertThat(output).contains(String.format("Cloning '%s'", USER))
                              .contains(String.format("Organization '%s' not found", USER))
                              .contains("Found user")
                              .contains("All done");
            assertThat(cloneDirectory).isDirectoryRecursivelyContaining(
                    String.format("glob:**/%s/Hello-World/README", USER));
        }
    }
}
