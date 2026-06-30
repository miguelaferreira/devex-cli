package devex.git;

/**
 * Signals that a repository was not cloned because a clone already exists at the
 * target location. This is an expected, benign outcome (not a failure) and lets
 * callers report it with an informational message rather than a warning.
 */
public class RepositoryAlreadyClonedException extends Exception {

    public RepositoryAlreadyClonedException(String repositoryName) {
        super(String.format("Repository '%s' is already cloned", repositoryName));
    }
}
