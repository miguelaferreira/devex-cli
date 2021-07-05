package devex;


import devex.git.GitCloneProtocol;
import devex.git.GitRepository;
import devex.git.GitService;
import devex.github.GithubOrganization;
import devex.github.GithubRepository;
import devex.github.GithubService;
import io.micronaut.context.annotation.Value;
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
                "Clone an entire GitHub organization with all repositories.",
                "While cloning, initialize project git sub-modules if option@|bold,underline -r|@ is provided.",
                "When a project is already cloned, tries to initialize git sub-modules if options@|bold,underline -r|@ is provided."
        }
)
public class GithubCloneCommand implements Runnable {

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

    @CommandLine.Parameters(
            index = "0",
            paramLabel = "ORG",
            description = "The GitHub organization to clone."
    )
    private String githubOrg;

    @CommandLine.Parameters(
            index = "1",
            paramLabel = "PATH",
            description = "The local path where to create the organization clone.",
            defaultValue = ".",
            showDefaultValue = CommandLine.Help.Visibility.ALWAYS
    )
    private String localPath;

    @Inject
    GithubService githubService;
    @Inject
    GitService gitService;
    @Value("${github.token}")
    String token;

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
        log.info("Cloning organization '{}'", githubOrg);

        final Either<String, GithubOrganization> maybeOrganization = githubService.getOrganization(githubOrg);
        if (maybeOrganization.isLeft()) {
            log.info("Could not find organization '{}': {}", githubOrg, maybeOrganization.getLeft());
            return;
        }

        final GithubOrganization organization = maybeOrganization.get();
        log.debug("Found organization = {}", organization);

        final Flowable<Tuple2<GithubRepository, Either<Throwable, Git>>> clonedRepositories =
                githubService.getOrganizationRepositories(organization.getName())
                             .map(repository -> Tuple.of(repository, buildGitRepository(repository)))
                             .map(tuple -> tuple.map2(
                                     repository -> recurseSubmodules
                                             ? gitService.cloneOrInitSubmodules(repository, localPath)
                                             : gitService.clone(repository, localPath)
                                     )
                             );

        clonedRepositories.blockingIterable()
                          .forEach(tuple -> {
                              final GithubRepository repository = tuple._1;
                              final Either<Throwable, Git> gitRepoOrError = tuple._2;
                              if (gitRepoOrError.isLeft()) {
                                  log.warn("Git operation failed", gitRepoOrError.getLeft());
                              } else {
                                  log.info("Repository '{}' updated.", repository.getFullName());
                              }
                          });

        log.info("All done");
    }

    private GitRepository buildGitRepository(GithubRepository repository) {
        return GitRepository.builder()
                            .name(repository.getFullName())
                            .path(repository.getFullName())
                            .cloneUrlSsh(repository.getSshUrl())
                            .cloneUrlHttps(repository.getHttpsUrl())
                            .build();
    }
}
