package devex.terraform;

import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Arrays;

@Slf4j
public class TerraformModuleDirectory {

    private final Path path;

    public TerraformModuleDirectory(Path path) {
        this.path = path;
    }

    public Either<String, String> execute(String executablePath, String[] arguments) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        PumpStreamHandler psh = new PumpStreamHandler(stdout);
        Executor executor = new DefaultExecutor();
        executor.setStreamHandler(psh);
        executor.setWorkingDirectory(path.toFile());
        final CommandLine commandLine = new CommandLine(executablePath);
        commandLine.addArguments(arguments);
        final Either<String, Integer> maybeExitValue = Try.of(() -> executor.execute(commandLine))
                                                          .toEither()
                                                          .mapLeft(t -> CommandUtils.handleOperationException(t, operationDescription(arguments), log));
        log.trace("terraform cmd {} :: \n{}", arguments, stdout);
        return maybeExitValue.flatMap(exitValue -> exitValue == 0 ? Either.right(stdout.toString()) : Either.left("Error: exitValue = " + exitValue + " :: " + stdout));
    }

    public Flux<Either<String, String>> executeAsync(String executablePath, String[] arguments) {
        return Flux.just(execute(executablePath, arguments));
    }

    private String operationDescription(String[] arguments) {
        return "executing terraform with " + List.ofAll(Arrays.stream(arguments)).mkString("[", ", ", "]");
    }
}
