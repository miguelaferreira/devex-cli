package devex.git;

import devex.gitlab.GitlabProject;
import io.micronaut.context.annotation.Value;
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
    @Value("${gitlab.token:}")
    private String httpsPassword = "";

    public void setCloneProtocol(GitCloneProtocol cloneProtocol) {
        this.cloneProtocol = cloneProtocol;
    }

    protected void setHttpsPassword(String httpsPassword) {
        this.httpsPassword = httpsPassword;
    }

    public Either<Throwable, Git> cloneOrInitSubmodulesProject(GitlabProject project, String cloneDirectory) {
        final String projectName = project.getNameWithNamespace();
        log.trace("Cloning or initializing submodules for project '{}' under directory '{}'", projectName, cloneDirectory);
        return tryCloneProject(project, cloneDirectory, projectName, true)
                .recoverWith(t -> tryInitSubmodules(project, cloneDirectory))
                .toEither();
    }

    public Either<Throwable, Git> cloneProject(final GitlabProject project, final String cloneDirectory) {
        final String projectName = project.getNameWithNamespace();
        log.trace("Cloning project '{}' under directory '{}'", projectName, cloneDirectory);
        return tryCloneProject(project, cloneDirectory, projectName, false)
                .toEither();
    }

    private Try<Git> tryCloneProject(final GitlabProject project, final String cloneDirectory, final String projectName, final boolean cloneSubmodules) {
        return Try.of(() -> cloneProject(project, cloneDirectory, cloneSubmodules))
                  .onSuccess(gitRepo -> log.trace("Cloned project '{}' to '{}'", projectName, getDirectory(gitRepo)))
                  .onFailure(t -> logFailedClone(projectName, t));
    }

    private Try<Git> tryInitSubmodules(GitlabProject project, String cloneDirectory) {
        return Try.of(() -> initSubmodules(project, cloneDirectory))
                  .onSuccess(gitRepo -> log.trace("Initialized submodules of git repository at '{}'", getDirectory(gitRepo)))
                  .onFailure(t2 -> logFailedSubmoduleInit(project.getName(), t2));
    }

    protected Git openRepository(GitlabProject project, String cloneDirectory) throws IOException {
        String pathToRepo = cloneDirectory + FileSystems.getDefault().getSeparator() + project.getPathWithNamespace();
        return Git.open(new File(pathToRepo));
    }

    private void logFailedClone(String projectName, Throwable throwable) {
        log.debug(String.format("Could not clone project '%s' because: %s", projectName, throwable.getMessage()), throwable);
    }

    private String getDirectory(Git gitRepo) {
        return gitRepo.getRepository().getDirectory().toString();
    }

    protected Git cloneProject(GitlabProject project, String cloneDirectory, boolean cloneSubmodules) throws GitAPIException {
        String pathToClone = cloneDirectory + FileSystems.getDefault().getSeparator() + project.getPathWithNamespace();

        final CloneCommand cloneCommand = Git.cloneRepository();
        switch (cloneProtocol) {
            case SSH:
                cloneCommand.setURI(project.getSshUrlToRepo());
                cloneCommand.setTransportConfigCallback(transport -> {
                    SshTransport sshTransport = (SshTransport) transport;
                    sshTransport.setSshSessionFactory(sshSessionFactory);
                });
                break;
            case HTTPS:
                cloneCommand.setURI(project.getHttpUrlToRepo());
                final String password = Objects.requireNonNullElse(httpsPassword, "");
                if (!password.isBlank()) {
                    cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(HTTPS_USERNAME, httpsPassword));
                } else {
                    log.debug("Credentials for HTTPS remote not set, group to clone must be public.");
                }
                break;
        }
        cloneCommand.setDirectory(new File(pathToClone));
        cloneCommand.setCloneSubmodules(cloneSubmodules);
        cloneCommand.setCloneAllBranches(false);
        cloneCommand.setNoTags();

        return cloneCommand.call();
    }

    protected Git initSubmodules(GitlabProject project, String cloneDirectory) throws IOException, GitAPIException {
        final Git repo = openRepository(project, cloneDirectory);
        repo.submoduleInit().call();
        repo.submoduleUpdate().call();
        return repo;
    }

    private void logFailedSubmoduleInit(String projectName, Throwable throwable) {
        log.debug(String.format("Could not initialize submodules for project '%s' because: %s", projectName, throwable.getMessage()), throwable);
    }
}
