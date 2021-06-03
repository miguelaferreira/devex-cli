package devex;

import ch.qos.logback.core.joran.spi.JoranException;
import io.micronaut.context.ApplicationContext;
import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class GitlabCloneCommandWithTokenTest extends GitlabCloneCommandBase {

    @TempDir
    File cloneDirectory;
    private ByteArrayOutputStream baos;

    @BeforeAll
    static void beforeAll() throws JoranException {
        // Need to do this because configuration is static and loaded once per JVM,
        // if the full logs test runs before this one the full log configuration
        // remains active.
        LoggingConfiguration.loadLogsConfig();
    }

    @Test
    public void run_publicGroup_withoutRecursion_verbose() {
        ByteArrayOutputStream baos = redirectOutput();

        try (ApplicationContext ctx = buildApplicationContext()) {
            String[] args = new String[]{"gitlab", "clone", "-v", PUBLIC_GROUP_NAME, cloneDirectory.toPath().toString()};
            DevexCommand.execute(ctx, args);

            assertLogsDebug(baos.toString(), PUBLIC_GROUP_NAME, PUBLIC_GROUP_NAME)
                    .contains(String.format("Looking for group named: %s", PUBLIC_GROUP_NAME));
            assertCloneContentsPublicGroup(cloneDirectory, false);
            final Path submodulePath = Path.of(cloneDirectory.getAbsolutePath(), "gitlab-clone-example", "a-project", "some-project-sub-module");
            assertThat(submodulePath).isEmptyDirectory();
        }
    }

    @Test
    public void run_privateGroup_withoutRecursion_VeryVerbose() {
        ByteArrayOutputStream baos = redirectOutput();

        try (ApplicationContext ctx = buildApplicationContext()) {
            String[] args = new String[]{"gitlab", "clone", "-x", PRIVATE_GROUP_NAME, cloneDirectory.toPath().toString()};
            DevexCommand.execute(ctx, args);

            final AbstractStringAssert<?> testAssert = assertLogsTrace(baos.toString(), PRIVATE_GROUP_NAME);
            assertLogsTraceWhenGroupFound(testAssert, PRIVATE_GROUP_NAME);
            assertCloneContentsPrivateGroup(cloneDirectory);
        }
    }

    @Test
    public void run_publicGroup_withRecursion_veryVerbose() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(TEST_OUTPUT_ARRAY_SIZE);
        System.setOut(new PrintStream(baos));

        try (ApplicationContext ctx = buildApplicationContext()) {
            String[] args = new String[]{"gitlab", "clone", "-x", "-r", PUBLIC_GROUP_NAME, cloneDirectory.toPath().toString()};
            DevexCommand.execute(ctx, args);

            final AbstractStringAssert<?> testAssert = assertLogsTrace(baos.toString(), PUBLIC_GROUP_NAME);
            assertLogsTraceWhenGroupFound(testAssert, PUBLIC_GROUP_NAME);
            assertCloneContentsPublicGroup(cloneDirectory, true);
        }
    }
}
