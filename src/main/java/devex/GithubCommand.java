package devex;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "github",
        aliases = "gh",
        header = {
                "GitHub tools."
        },
        subcommands = {GithubCloneCommand.class},
        descriptionHeading = "%nGitHub configuration:%n%n",
        description = {
                "The GitHub URL and private token are read from the environment, using GItHUB_URL and GITHUB_TOKEN variables.",
                "GItHUB_URL defaults to 'https://api.github.com'.",
                "The GitHub token is used for both querying the GitHub API to discover the organization to clone and as the password for cloning using HTTPS.",
                "No token is needed for public groups and repositories."
        },
        scope = CommandLine.ScopeType.INHERIT
)
public class GithubCommand {
}
