package devex;

import javax.inject.Inject;
import java.nio.file.Path;

import devex.terraform.TerraformModuleDirectory;
import devex.terraform.TerraformStateService;
import io.reactivex.Flowable;
import io.vavr.control.Either;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Slf4j
@Command(
        name = "taint-secrets",
        aliases = "ts",
        header = {
                "Taint all resources with secrets in the terraform state of a module.",
                "Resources are compared against a pre-defined list of resources that are known to contain secrets.",
                "The predefined list of resources can be overwritten on the environment via the TERRAFORM_SECRETS variable " +
                        "(eg. TERRAFORM_SECRETS=\"aws_iam_access_key,tls_private_key\").",
                "Tainting marks the resources for recreation, it does not actually recreate them.",
                "The process can be reverted and the resources untainted if option @|bold,underline -u|@ is provided.",
                "To rotate secrets, run terraform apply after tainting the resources.",
        }
)
public class TerraformTaintSecretsCommand implements Runnable {

    @CommandLine.Option(
            order = 0,
            names = {"-u", "--untaint"},
            description = "Untaint resources.",
            defaultValue = "false"
    )
    private boolean untaint;

    @CommandLine.Parameters(
            index = "0",
            paramLabel = "MODULE",
            description = "The terraform module to manipulate."
    )
    private String terraformModule;

    @Inject
    TerraformStateService terraformStateService;

    @Override
    public void run() {
        final String operationName = operationName();
        log.info("{}ing terraform resources in module '{}'", operationName, terraformModule);
        final Flowable<Either<String, Flowable<Either<String, String>>>> execution =
                terraformStateService.taintSecretsAsync(new TerraformModuleDirectory(Path.of(terraformModule)), untaint);
        final Either<String, Flowable<Either<String, String>>> maybeTaintedResources = execution.blockingFirst();
        if (maybeTaintedResources.isLeft()) {
            log.error("{} operation failed: {}", operationName, maybeTaintedResources.getLeft());
        } else {
            final Flowable<Either<String, String>> taintedResources = maybeTaintedResources.get();
            taintedResources.blockingIterable()
                            .forEach(maybeTaintedResource -> {
                                if (maybeTaintedResource.isLeft()) {
                                    log.warn("{} failed: {}", operationName, maybeTaintedResource.getLeft().trim());
                                } else {
                                    log.info("{}: {}", operationName, maybeTaintedResource.get().trim());
                                }
                            });
            if (!untaint) {
                log.info("To rotate the secrets run terraform apply on the module");
            }
            log.info("All done");
        }
    }

    private String operationName() {
        return untaint ? "Untaint" : "Taint";
    }
}
