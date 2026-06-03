package devex;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;

@Tag("ssh-integration")
@EnabledIfEnvironmentVariable(named = "DEVEX_SSH_INTEGRATION_TESTS", matches = "true",
        disabledReason = "Clone tests use the SSH protocol; set DEVEX_SSH_INTEGRATION_TESTS=true to run.")
public class GitlabCloneCommandFullLogs extends GitlabCloneCommandBase {

    @TempDir
    File cloneDirectory;

    @Test
    public void run_publicGroup_debug() {
        ByteArrayOutputStream baos = redirectOutput();

        try (ApplicationContext ctx = buildApplicationContext()) {
            String[] args = new String[]{"gitlab", "clone", "--debug", PUBLIC_GROUP_NAME, cloneDirectory.toPath().toString()};
            DevexCommand.execute(ctx, args);

            assertDebugFullLogs(baos.toString(), PUBLIC_GROUP_NAME);
            assertCloneContentsPublicGroup(cloneDirectory, false);
        }
    }

    @Test
    public void run_privateGroup_trace() {
        ByteArrayOutputStream baos = redirectOutput();

        try (ApplicationContext ctx = buildApplicationContext()) {
            String[] args = new String[]{"gitlab", "clone", "--trace", PRIVATE_GROUP_NAME, cloneDirectory.toPath().toString()};
            DevexCommand.execute(ctx, args);

            assertTraceFullLogs(baos.toString(), PRIVATE_GROUP_NAME);
            assertCloneContentsPrivateGroup(cloneDirectory);
        }
    }
}
