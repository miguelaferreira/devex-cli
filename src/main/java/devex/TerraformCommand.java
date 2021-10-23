package devex;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "terraform",
        aliases = "tf",
        subcommands = {TerraformTaintSecretsCommand.class},
        descriptionHeading = "%nTerraform configuration:%n%n",
        description = {
                "Any credentials needed to access the terraform state are either read from the environment and passed along to the terraform command, " +
                        "or configured in the terraform module being manipulated.",
                "The expected location for the terraform command is '/usr/local/bin/terraform', " +
                        "and it can be overwritten on the environment using variable TERRAFORM_COMMAND."
        },
        scope = CommandLine.ScopeType.INHERIT
)
public class TerraformCommand {
}
