package devex.git;

import io.vavr.control.Either;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Logs the outcome of a clone operation consistently across clone commands.
 */
public final class GitCloneResults {

    private GitCloneResults() {
    }

    /**
     * Logs the result of a clone using the caller's logger so the log line keeps
     * the calling command's logger name.
     *
     * @param log            the caller's logger
     * @param result         the clone result; a right holds the cloned repository, a left the failure cause
     * @param successMessage supplies the message logged at INFO when the clone succeeded
     */
    public static void log(final Logger log, final Either<Throwable, Git> result, final Supplier<String> successMessage) {
        if (result.isRight()) {
            log.info(successMessage.get());
            return;
        }
        final Throwable error = result.getLeft();
        if (error instanceof RepositoryAlreadyClonedException) {
            log.info(error.getMessage());
        } else {
            log.warn("Git operation failed", error);
        }
    }
}
