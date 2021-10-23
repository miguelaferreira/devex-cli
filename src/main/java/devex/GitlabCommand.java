package devex;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "gitlab",
        aliases = "gl",
        header = {
                "GitLab tools, saving time by automating gruntwork."
        },
        subcommands = {GitlabCloneCommand.class},
        descriptionHeading = "%nGitLab configuration:%n%n",
        description = {
                "The GitLab URL and private token are read from the environment, using GITLAB_URL and GITLAB_TOKEN variables.",
                "GITLAB_URL defaults to 'https://gitlab.com'.",
                "The GitLab token is used for both querying the GitLab API to discover the group to clone and as the password for cloning using HTTPS.",
                "No token is needed for public groups and repositories."
        },
        scope = CommandLine.ScopeType.INHERIT
)
public class GitlabCommand {
}
