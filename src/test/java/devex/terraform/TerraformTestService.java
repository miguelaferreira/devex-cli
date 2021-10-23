package devex.terraform;

import io.vavr.control.Either;

public class TerraformTestService {

    private final TerraformCommand command = new TerraformCommand("terraform");

    public Either<String, String> terraformInit(TerraformModuleDirectory directory) {
        return command.execute(directory, new String[]{"init", "-input=false"});
    }

    public Either<String, String> terraformApply(TerraformModuleDirectory directory) {
        return command.execute(directory, new String[]{"apply", "-auto-approve"});
    }

    public Either<String, String> terraformDestroy(TerraformModuleDirectory directory) {
        return command.execute(directory, new String[]{"destroy", "-auto-approve"});
    }
}
