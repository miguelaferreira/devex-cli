package devex;

import ch.qos.logback.core.joran.spi.JoranException;
import io.micronaut.context.ApplicationContext;
import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;

public class GitlabCloneCommandWithoutTokenTest extends GitlabCloneCommandBase {

    @TempDir
    File cloneDirectory;

    @BeforeAll
    static void beforeAll() throws JoranException {
        // Need to do this because configuration is static and loaded once per JVM,
        // if the full logs test runs before this one the full log configuration
        // remains active.
        LoggingConfiguration.loadLogsConfig();
    }

    @Test
    public void run_publicGroup_withRecursion_verbose() {
        ByteArrayOutputStream baos = redirectOutput();

        try (ApplicationContext ctx = buildApplicationContext(NO_TOKEN_CONTEXT_PROPERTIES)) {
            String[] args = new String[]{"gitlab", "clone", "-v", "-r", PUBLIC_GROUP_NAME, cloneDirectory.toPath().toString()};
            DevexCommand.execute(ctx, args);

            assertLogsDebug(baos.toString(), PUBLIC_GROUP_NAME, PUBLIC_GROUP_NAME)
                    .contains(String.format("Looking for group named: %s", PUBLIC_GROUP_NAME));
            assertCloneContentsPublicGroup(cloneDirectory, true);
        }
    }

    @Test
    public void run_publicSubGroupByPath_withRecursion_verbose() {
        ByteArrayOutputStream baos = redirectOutput();

        try (ApplicationContext ctx = buildApplicationContext(NO_TOKEN_CONTEXT_PROPERTIES)) {
            String[] args = new String[]{"gitlab", "clone", "-v", "-r", "-m", "full_path", PUBLIC_SUB_GROUP_FULL_PATH, cloneDirectory
                    .toPath().toString()};
            DevexCommand.execute(ctx, args);

            assertLogsDebug(baos.toString(), PUBLIC_SUB_GROUP_FULL_PATH, PUBLIC_SUB_GROUP_FULL_PATH)
                    .contains(String.format("Looking for group with full path: %s", PUBLIC_SUB_GROUP_FULL_PATH));
            assertCloneContentsPublicSubGroup(cloneDirectory);
        }
    }

    @Test
    public void run_publicGroupById_withRecursion_verbose() {
        ByteArrayOutputStream baos = redirectOutput();

        try (ApplicationContext ctx = buildApplicationContext(NO_TOKEN_CONTEXT_PROPERTIES)) {
            String[] args = new String[]{"gitlab", "clone", "-v", "-r", "-m", "id", PUBLIC_GROUP_ID, cloneDirectory.toPath().toString()};
            DevexCommand.execute(ctx, args);

            assertLogsDebug(baos.toString(), PUBLIC_GROUP_ID, PUBLIC_GROUP_NAME)
                    .contains(String.format("Looking for group identified by: %s", PUBLIC_GROUP_ID));
            assertCloneContentsPublicGroup(cloneDirectory, true);
        }
    }

    @Test
    public void run_privateGroup_withoutRecursion_veryVerbose() {
        ByteArrayOutputStream baos = redirectOutput();

        try (ApplicationContext ctx = buildApplicationContext(NO_TOKEN_CONTEXT_PROPERTIES)) {
            String[] args = new String[]{"gitlab", "clone", "-x", PRIVATE_GROUP_NAME, cloneDirectory.toPath().toString()};
            DevexCommand.execute(ctx, args);

            final AbstractStringAssert<?> testAssert = assertLogsTrace(baos.toString(), PRIVATE_GROUP_NAME);
            assertLogsTraceWhenGroupNotFound(testAssert, PRIVATE_GROUP_NAME);
            assertNotCloned(cloneDirectory);
        }
    }
}
