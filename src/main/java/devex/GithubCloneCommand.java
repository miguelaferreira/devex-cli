package devex;


import devex.git.GitCloneProtocol;
import devex.git.GitCloneResults;
import devex.git.GitRepository;
import devex.git.GitService;
import devex.github.GithubOrganization;
import devex.github.GithubRepository;
import devex.github.GithubService;
import io.micronaut.context.annotation.Value;
import reactor.core.publisher.Flux;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.control.Either;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import jakarta.inject.Inject;

@Slf4j
@Command(
        name = "clone",
        aliases = "cl",
        header = {
                "Clone an entire GitHub organization with all repositories.",
                "When no organization matches the given name, clone all repositories of the GitHub user with that name instead.",
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
            description = "The GitHub organization to clone. If no organization matches, it is treated as a GitHub user."
    )
    private String githubOrg;

    @CommandLine.Parameters(
            index = "1",
            paramLabel = "PATH",
            description = "The local path where to create the organization (or user) clone.",
            defaultValue = ".",
            showDefaultValue = CommandLine.Help.Visibility.ALWAYS
    )
    private String localPath;

    @Inject
    GithubService githubService;
    @Inject
    GitService gitService;
    @Value("${github.token:}")
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
        log.info("Cloning '{}'", githubOrg);

        final Either<String, Flux<GithubRepository>> maybeRepositories = resolveRepositories();
        if (maybeRepositories.isLeft()) {
            log.info("Could not find organization or user '{}': {}", githubOrg, maybeRepositories.getLeft());
            return;
        }

        final Flux<Tuple2<GithubRepository, Either<Throwable, Git>>> clonedRepositories =
                maybeRepositories.get()
                             .map(repository -> Tuple.of(repository, buildGitRepository(repository)))
                             .map(tuple -> tuple.map2(
                                             repository -> recurseSubmodules
                                                     ? gitService.cloneOrInitSubmodules(repository, localPath)
                                                     : gitService.clone(repository, localPath)
                                     )
                             );

        clonedRepositories.toIterable()
                          .forEach(tuple -> {
                              final GithubRepository repository = tuple._1;
                              final Either<Throwable, Git> gitRepoOrError = tuple._2;
                              GitCloneResults.log(log, gitRepoOrError,
                                      () -> String.format("Repository '%s' updated.", repository.getFullName()));
                          });

        log.info("All done.");
    }

    private Either<String, Flux<GithubRepository>> resolveRepositories() {
        final Either<String, GithubOrganization> maybeOrganization = githubService.getOrganization(githubOrg);
        if (maybeOrganization.isRight()) {
            final GithubOrganization organization = maybeOrganization.get();
            log.debug("Found organization = {}", organization);
            return Either.right(githubService.getOrganizationRepositories(organization.getName()));
        }

        log.debug("Organization '{}' not found ({}), trying user", githubOrg, maybeOrganization.getLeft());
        final Either<String, GithubOrganization> maybeUser = githubService.getUser(githubOrg);
        if (maybeUser.isRight()) {
            final GithubOrganization user = maybeUser.get();
            log.debug("Found user = {}", user);
            return Either.right(githubService.getUserRepositories(user.getName()));
        }

        return Either.left(maybeUser.getLeft());
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
