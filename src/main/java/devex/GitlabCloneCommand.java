package devex;


import devex.git.GitCloneProtocol;
import devex.git.GitService;
import devex.gitlab.GitlabGroup;
import devex.gitlab.GitlabGroupSearchMode;
import devex.gitlab.GitlabProject;
import devex.gitlab.GitlabService;
import io.reactivex.Flowable;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import javax.inject.Inject;

@Slf4j
@Command(
        name = "clone",
        aliases = "cl",
        header = {
                "Clone an entire GitLab group with all sub-groups and repositories.",
                "While cloning initialize project git sub-modules (may require two runs due to ordering of projects).",
                "When a project is already cloned, tries to initialize git sub-modules."
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

    @CommandLine.Option(
            order = 3,
            names = {"-u", "--https-username"},
            description = "The username to authenticate with when the HTTPS clone protocol is selected. This option is required when cloning private groups, in which case the GitLab token will be used as the password.",
            arity = "0..1",
            interactive = true
    )
    private String httpsUsername;

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

    @Inject
    GitlabService gitlabService;
    @Inject
    GitService gitService;

    @Override
    public void run() {
        configureGitService();
        cloneGroup();
    }

    private void configureGitService() {
        gitService.setCloneProtocol(cloneProtocol);
        gitService.setHttpsUsername(httpsUsername);
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

        final Flowable<Tuple2<GitlabProject, Either<Throwable, Git>>> clonedProjects =
                gitlabService.getGitlabGroupProjects(group)
                             .map(project -> Tuple.of(project, project))
                             .map(tuple -> tuple.map2(
                                     project -> recurseSubmodules
                                             ? gitService.cloneOrInitSubmodulesProject(project, localPath)
                                             : gitService.cloneProject(project, localPath)
                                     )
                             );

        clonedProjects.blockingIterable()
                      .forEach(tuple -> {
                          final GitlabProject project = tuple._1;
                          final Either<Throwable, Git> gitRepoOrError = tuple._2;
                          if (gitRepoOrError.isLeft()) {
                              log.warn("Git operation failed", gitRepoOrError.getLeft());
                          } else {
                              log.info("Project '{}' updated.", project.getNameWithNamespace());
                          }
                      });

        log.info("All done");
    }
}
