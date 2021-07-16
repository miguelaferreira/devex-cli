package devex.git;

import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Objects;

@Slf4j
@Singleton
public class GitService {

    private static final SshSessionFactory sshSessionFactory = new OverrideJschConfigSessionFactory();
    public static final String HTTPS_USERNAME = "git";

    private GitCloneProtocol cloneProtocol = GitCloneProtocol.SSH;
    private String httpsPassword = "";

    public void setCloneProtocol(GitCloneProtocol cloneProtocol) {
        this.cloneProtocol = cloneProtocol;
    }

    public void setHttpsPassword(String httpsPassword) {
        this.httpsPassword = httpsPassword;
    }

    public Either<Throwable, Git> cloneOrInitSubmodules(GitRepository repository, String cloneDirectory) {
        final String repositoryName = repository.getName();
        log.trace("Cloning or initializing submodules for repository '{}' under directory '{}'", repositoryName, cloneDirectory);
        return tryClone(repository, cloneDirectory, true)
                .recoverWith(t -> tryInitSubmodules(repository, cloneDirectory))
                .toEither();
    }

    public Either<Throwable, Git> clone(final GitRepository repository, final String cloneDirectory) {
        final String repositoryName = repository.getName();
        log.trace("Cloning repository '{}' under directory '{}'", repositoryName, cloneDirectory);
        return tryClone(repository, cloneDirectory, false)
                .toEither();
    }

    private Try<Git> tryClone(final GitRepository repository, final String cloneDirectory, final boolean cloneSubmodules) {
        String repositoryName = repository.getName();
        return Try.of(() -> clone(repository, cloneDirectory, cloneSubmodules))
                  .onSuccess(gitRepo -> log.trace("Cloned repository '{}' to '{}'", repositoryName, getDirectory(gitRepo)))
                  .onFailure(t -> logFailedClone(repositoryName, t));
    }

    private Try<Git> tryInitSubmodules(GitRepository repository, String cloneDirectory) {
        return Try.of(() -> initSubmodules(repository, cloneDirectory))
                  .onSuccess(gitRepo -> log.trace("Initialized submodules of git repository at '{}'", getDirectory(gitRepo)))
                  .onFailure(t2 -> logFailedSubmoduleInit(repository.getName(), t2));
    }

    protected Git openRepository(GitRepository repository, String cloneDirectory) throws IOException {
        String pathToRepo = cloneDirectory + FileSystems.getDefault().getSeparator() + repository.getPath();
        return Git.open(new File(pathToRepo));
    }

    private void logFailedClone(String repositoryName, Throwable throwable) {
        log.debug(String.format("Could not clone repository '%s' because: %s", repositoryName, throwable.getMessage()), throwable);
    }

    private String getDirectory(Git gitRepo) {
        return gitRepo.getRepository().getDirectory().toString();
    }

    protected Git clone(GitRepository repository, String cloneDirectory, boolean cloneSubmodules) throws GitAPIException {
        String pathToClone = cloneDirectory + FileSystems.getDefault().getSeparator() + repository.getPath();

        final CloneCommand cloneCommand = Git.cloneRepository();
        switch (cloneProtocol) {
            case SSH:
                cloneCommand.setURI(repository.getCloneUrlSsh());
                cloneCommand.setTransportConfigCallback(transport -> {
                    SshTransport sshTransport = (SshTransport) transport;
                    sshTransport.setSshSessionFactory(sshSessionFactory);
                });
                break;
            case HTTPS:
                cloneCommand.setURI(repository.getCloneUrlHttps());
                final String password = Objects.requireNonNullElse(httpsPassword, "");
                if (!password.isBlank()) {
                    cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(HTTPS_USERNAME, httpsPassword));
                } else {
                    log.debug("Credentials for HTTPS remote not set, repository to clone must be public.");
                }
                break;
        }
        cloneCommand.setDirectory(new File(pathToClone));
        cloneCommand.setCloneSubmodules(cloneSubmodules);
        cloneCommand.setCloneAllBranches(false);
        cloneCommand.setNoTags();

        return cloneCommand.call();
    }

    protected Git initSubmodules(GitRepository repository, String cloneDirectory) throws IOException, GitAPIException {
        final Git repo = openRepository(repository, cloneDirectory);
        repo.submoduleInit().call();
        repo.submoduleUpdate().call();
        return repo;
    }

    private void logFailedSubmoduleInit(String repositoryName, Throwable throwable) {
        log.debug(String.format("Could not initialize submodules for repository '%s' because: %s", repositoryName, throwable
                .getMessage()), throwable);
    }
}
