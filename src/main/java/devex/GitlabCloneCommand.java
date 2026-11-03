package devex;

import devex.git.GitCloneProtocol;
import devex.git.GitCloneResults;
import devex.git.GitRepository;
import devex.git.GitService;
import devex.gitlab.GitlabGroup;
import devex.gitlab.GitlabGroupSearchMode;
import devex.gitlab.GitlabProject;
import devex.gitlab.GitlabService;
import io.micronaut.context.annotation.Value;
import io.vavr.control.Either;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Slf4j
@Command(
        name = "clone",
        aliases = "cl",
        header = {
                "Clone an entire GitLab group with all sub-groups and repositories.",
                "While cloning initialize project git sub-modules if option @|bold,underline -r|@ is provided.",
                "When a project is already cloned, tries to initialize git sub-modules if option @|bold,underline -r|@ is provided."
        }
)
public class GitlabCloneCommand implements Runnable {

    @CommandLine.Option(
            order = 0,
            names = {"-r", "--recurse-submodules"},
            description = "Initialize project submodules. If projects are already cloned try and initialize sub-modules anyway.",
            defaultValue = "false"
    )
    private boolean recurseSubmodules;

    @CommandLine.Option(
            order = 1,
            names = {"-c", "--clone-protocol"},
            description = "Chose the transport protocol used clone the project repositories. Valid values: ${COMPLETION-CANDIDATES}.",
            defaultValue = "SSH",
            showDefaultValue = CommandLine.Help.Visibility.ALWAYS
    )
    private GitCloneProtocol cloneProtocol;

    @CommandLine.Option(
            order = 2,
            names = {"-m", "--search-mode"},
            description = "Chose how the group is searched for. Groups can be searched by name or full path. Valid values: ${COMPLETION-CANDIDATES}.",
            defaultValue = "NAME",
            showDefaultValue = CommandLine.Help.Visibility.ALWAYS
    )
    private GitlabGroupSearchMode searchMode;

    @CommandLine.Parameters(
            index = "0",
            paramLabel = "GROUP",
            description = "The GitLab group to clone."
    )
    private String gitlabGroup;

    @CommandLine.Parameters(
            index = "1",
            paramLabel = "PATH",
            description = "The local path where to create the group clone.",
            defaultValue = ".",
            showDefaultValue = CommandLine.Help.Visibility.ALWAYS
    )
    private String localPath;

    private final GitlabService gitlabService;
    private final GitService gitService;
    private final String token;

    @Inject
    public GitlabCloneCommand(GitlabService gitlabService, GitService gitService, @Value("${gitlab.token:}") String token) {
        this.gitlabService = gitlabService;
        this.gitService = gitService;
        this.token = token;
    }

    @Override
    public void run() {
        configureGitService();
        cloneGroup();
    }

    private void configureGitService() {
        gitService.setCloneProtocol(cloneProtocol);
        if (cloneProtocol == GitCloneProtocol.HTTPS) {
            gitService.setHttpsPassword(token);
        }
    }

    private void cloneGroup() {
        log.info("Cloning group '{}'", gitlabGroup);

        final Either<String, GitlabGroup> maybeGroup = gitlabService.findGroupBy(gitlabGroup, searchMode);
        if (maybeGroup.isLeft()) {
            log.info("Could not find group '{}': {}", gitlabGroup, maybeGroup.getLeft());
            return;
        }

        final GitlabGroup group = maybeGroup.get();
        log.debug("Found group = {}", group);

        gitlabService.getGitlabGroupProjects(group)
                     .map(project -> new CloneOutcome(project, cloneProject(project)))
                     .toIterable()
                     .forEach(outcome -> GitCloneResults.log(log, outcome.result(),
                             () -> "Project '%s' updated.".formatted(outcome.project().nameWithNamespace())));

        log.info("All done.");
    }

    private Either<Throwable, Git> cloneProject(GitlabProject project) {
        final GitRepository repository = buildGitRepository(project);
        return recurseSubmodules
                ? gitService.cloneOrInitSubmodules(repository, localPath)
                : gitService.clone(repository, localPath);
    }

    private GitRepository buildGitRepository(GitlabProject project) {
        return GitRepository.builder()
                            .name(project.nameWithNamespace())
                            .path(project.pathWithNamespace())
                            .cloneUrlSsh(project.sshUrlToRepo())
                            .cloneUrlHttps(project.httpUrlToRepo())
                            .build();
    }

    private record CloneOutcome(GitlabProject project, Either<Throwable, Git> result) {
    }
}
