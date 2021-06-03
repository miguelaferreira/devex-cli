package devex;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import org.assertj.core.api.AbstractFileAssert;
import org.assertj.core.api.AbstractStringAssert;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class GitlabCloneCommandBase {

    public static final String PUBLIC_GROUP_NAME = "gitlab-clone-example";
    public static final String PUBLIC_GROUP_ID = "11961707";
    public static final String PRIVATE_GROUP_NAME = "gitlab-clone-example-private";
    public static final int TEST_OUTPUT_ARRAY_SIZE = 4096000;
    public static final Map<String, Object> NO_TOKEN_CONTEXT_PROPERTIES = Map.of("gitlab.token", "");
    public static final String PUBLIC_SUB_GROUP_FULL_PATH = "gitlab-clone-example/sub-group-2/sub-group-3";

    public ByteArrayOutputStream redirectOutput() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(TEST_OUTPUT_ARRAY_SIZE);
        System.setOut(new PrintStream(baos));
        return baos;
    }

    public ApplicationContext buildApplicationContext(Map<String, Object> contextProperties) {
        return ApplicationContext.run(contextProperties, Environment.CLI, Environment.TEST);
    }

    public ApplicationContext buildApplicationContext() {
        return ApplicationContext.run(Map.of(), Environment.CLI, Environment.TEST);
    }

    public void assertDebugFullLogs(String output, String groupName) {
        assertThat(output).contains("] DEBUG devex.DevexCommand - Set all loggers to DEBUG")
                          .contains(String.format("] INFO  devex.GitlabCloneCommand - Cloning group '%s'", groupName))
                          .contains(String.format("] DEBUG devex.gitlab.GitlabService - Looking for group named: %s", groupName))
                          .contains(String.format("] DEBUG devex.gitlab.GitlabService - Searching for projects in group '%s'", groupName))
                          .contains("] INFO  devex.GitlabCloneCommand - All done")
                          .doesNotContain("PRIVATE-TOKEN");
    }

    public void assertTraceFullLogs(String output, String groupName) {
        assertThat(output).contains("] TRACE devex.DevexCommand - Set all loggers to TRACE")
                          .contains(String.format("] INFO  devex.GitlabCloneCommand - Cloning group '%s'", groupName))
                          .contains(String.format("] DEBUG devex.gitlab.GitlabService - Looking for group named: %s", groupName))
                          .contains(String.format("] DEBUG devex.gitlab.GitlabService - Searching for projects in group '%s'", groupName))
                          .contains("] TRACE devex.gitlab.GitlabService - Invoking paginated API")
                          .contains("] INFO  devex.GitlabCloneCommand - All done")
                          .contains("PRIVATE-TOKEN");
    }

    public AbstractStringAssert<?> assertLogsDebug(String output, String group, String groupPath) {
        return assertThat(output).contains("Set application loggers to DEBUG")
                                 .contains(String.format("Cloning group '%s'", group))
                                 .contains(String.format("Searching for projects in group '%s'", groupPath))
                                 .contains("All done")
                                 .doesNotContain("PRIVATE-TOKEN")
                                 .doesNotContain("devex.GitlabCloneCommand");
    }

    public AbstractStringAssert<?> assertLogsTrace(String output, String groupName) {
        return assertThat(output).contains("Set application loggers to TRACE")
                                 .contains(String.format("Cloning group '%s'", groupName))
                                 .contains(String.format("Looking for group named: %s", groupName))
                                 .doesNotContain("devex.GitlabCloneCommand");
    }

    public AbstractStringAssert<?> assertLogsTraceWhenGroupFound(AbstractStringAssert<?> testAssert, String groupName) {
        return testAssert.contains(String.format("Searching for projects in group '%s'", groupName))
                         .contains("Invoking paginated API")
                         .contains("All done")
                         .doesNotContain("PRIVATE-TOKEN")
                         .doesNotContain("devex.GitlabCloneCommand");
    }

    public AbstractStringAssert<?> assertLogsTraceWhenGroupNotFound(AbstractStringAssert<?> testAssert, String groupName) {
        return testAssert.contains(String.format("Could not find group '%s': Group not found", groupName));
    }

    public void assertCloneContentsPublicGroup(File cloneDirectory, boolean withSubmodules) {
        final AbstractFileAssert<?> abstractFileAssert = assertThat(cloneDirectory);
        abstractFileAssert.isDirectoryContaining(String.format("glob:**/%s", PUBLIC_GROUP_NAME))
                          .isDirectoryRecursivelyContaining(String.format("glob:**/%s/a-project/README.md", PUBLIC_GROUP_NAME))
                          .isDirectoryRecursivelyContaining(String.format("glob:**/%s/sub-group-1/some-project/README.md", PUBLIC_GROUP_NAME))
                          .isDirectoryRecursivelyContaining(String.format("glob:**/%s/sub-group-2/sub-group-3/another-project/README.md", PUBLIC_GROUP_NAME));

        if (withSubmodules) {
            abstractFileAssert.isDirectoryRecursivelyContaining(String.format("glob:**/%s/a-project/some-project-sub-module/README.md", PUBLIC_GROUP_NAME));
        } else {
            final Path submodulePath = Path.of(cloneDirectory.getAbsolutePath(), PUBLIC_GROUP_NAME, "a-project", "some-project-sub-module");
            assertThat(submodulePath).isEmptyDirectory();
        }
    }

    public void assertCloneContentsPublicSubGroup(File cloneDirectory) {
        final AbstractFileAssert<?> abstractFileAssert = assertThat(cloneDirectory);
        abstractFileAssert.isDirectoryContaining(String.format("glob:**/%s", PUBLIC_GROUP_NAME))
                          .isDirectoryRecursivelyContaining(String.format("glob:**/%s/sub-group-2/sub-group-3/another-project/README.md", PUBLIC_GROUP_NAME));
    }

    public void assertCloneContentsPrivateGroup(File cloneDirectory) {
        final AbstractFileAssert<?> abstractFileAssert = assertThat(cloneDirectory);

        abstractFileAssert.isDirectoryContaining(String.format("glob:**/%s", PRIVATE_GROUP_NAME))
                          .isDirectoryRecursivelyContaining(String.format("glob:**/%s/a-private-project/README.md", PRIVATE_GROUP_NAME))
                          .isDirectoryRecursivelyContaining(String.format("glob:**/%s/sub-group-1/another-private-project/README.md", PRIVATE_GROUP_NAME));
    }

    public void assertNotCloned(File cloneDirectory) {
        assertThat(cloneDirectory).isEmptyDirectory();
    }
}
