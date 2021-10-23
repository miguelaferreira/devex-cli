package devex;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "terraform",
        aliases = "tf",
        header = {
                "Terraform tools, saving time by automating gruntwork."
        },
        subcommands = {TerraformTaintSecretsCommand.class},
        descriptionHeading = "%nTerraform configuration:%n%n",
        description = {
                "Any credentials needed to access the terraform state are either read from the environment and passed along to the terraform command, " +
                        "or configured in the terraform module being manipulated.",
                "Terraform is expected to be available in the environment path, but if it is not, " +
                        "the full path to the command can be set on the environment using variable TERRAFORM_COMMAND."
        },
        scope = CommandLine.ScopeType.INHERIT
)
public class TerraformCommand {
}
