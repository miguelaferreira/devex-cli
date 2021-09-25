package devex.terraform;

import javax.inject.Singleton;

import io.micronaut.context.annotation.Value;
import io.reactivex.Flowable;
import io.vavr.control.Either;

@Singleton
public class TerraformCommand {
    private final String executablePath;

    public TerraformCommand(@Value("${terraform.command}") String executablePath) {
        this.executablePath = executablePath;
    }

    public Either<String, String> execute(TerraformModuleDirectory directory, String[] arguments) {
        return directory.execute(executablePath, arguments);
    }

    public Flowable<Either<String, String>> executeAsync(TerraformModuleDirectory directory, String[] arguments) {
        return directory.executeAsync(executablePath, arguments);
    }
}
