package devex.git;

import devex.TestBase;
import io.vavr.collection.Stream;
import io.vavr.control.Either;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.submodule.SubmoduleStatus;
import org.eclipse.jgit.submodule.SubmoduleStatusType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jgit.submodule.SubmoduleStatusType.INITIALIZED;
import static org.eclipse.jgit.submodule.SubmoduleStatusType.UNINITIALIZED;

class GitServiceTest extends TestBase {

    @TempDir
    File cloneDirectory;
    private String cloneDirectoryPath;
    private final String gitlabBotPassword = System.getenv("GITLAB_TOKEN");

    @BeforeEach
    void setUp() {
        cloneDirectoryPath = this.cloneDirectory.toPath().toString();
    }

    @Test
    void clonePublicRepo_ssh_withSubmodule() throws GitAPIException {
        final GitRepository repository = GitRepository.builder()
                                                      .name("gitlab-clone-example / a-project")
                                                      .path("gitlab-clone-example/a-project")
                                                      .cloneUrlSsh("git@gitlab.com:gitlab-clone-example/a-project.git")
                                                      .build();

        final Git repo = new GitService().clone(repository, cloneDirectoryPath, true);

        assertThat(repo.log().call()).isNotEmpty();
        assertThat(repo.submoduleStatus().call()).containsKey("some-project-sub-module")
                                                 .allSatisfy(this::submoduleIsInitialized);
    }

    @Test
    void clonePublicRepo_http_withSubmodule() throws GitAPIException {
        final GitService gitService = new GitService();
        gitService.setCloneProtocol(GitCloneProtocol.HTTPS);
        final GitRepository repository = GitRepository.builder()
                                                      .name("gitlab-clone-example / a-project")
                                                      .path("gitlab-clone-example/a-project")
                                                      .cloneUrlHttps("https://gitlab.com/gitlab-clone-example/a-project.git")
                                                      .build();

        final Git repo = gitService.clone(repository, cloneDirectoryPath, true);

        assertThat(repo.log().call()).isNotEmpty();
        assertThat(repo.submoduleStatus().call()).containsKey("some-project-sub-module")
                                                 .allSatisfy(this::submoduleIsInitialized);
    }

    @Test
    void clonePrivateRepo_ssh_withSubmodule() throws GitAPIException {
        final GitRepository repository = GitRepository.builder()
                                                      .name("gitlab-clone-example / a-private-project")
                                                      .path("gitlab-clone-example/a-private-project")
                                                      .cloneUrlSsh("git@gitlab.com:gitlab-clone-example-private/a-private-project.git")
                                                      .build();

        final Git repo = new GitService().clone(repository, cloneDirectoryPath, true);

        assertThat(repo.log().call()).isNotEmpty();
    }

    @Test
    void clonePrivateRepo_http_withSubmodule() throws GitAPIException {
        final GitService gitService = new GitService();
        gitService.setCloneProtocol(GitCloneProtocol.HTTPS);
        gitService.setHttpsPassword(gitlabBotPassword);
        final GitRepository repository = GitRepository.builder()
                                                      .name("gitlab-clone-example / a-private-project")
                                                      .path("gitlab-clone-example/a-private-project")
                                                      .cloneUrlHttps("https://gitlab.com/gitlab-clone-example-private/a-private-project.git")
                                                      .build();

        final Git repo = gitService.clone(repository, cloneDirectoryPath, true);

        assertThat(repo.log().call()).isNotEmpty();
    }

    @Test
    void clonePublicRepo_ssh_withoutSubmodule() throws GitAPIException {
        final GitRepository repository = GitRepository.builder()
                                                      .name("gitlab-clone-example / a-project")
                                                      .path("gitlab-clone-example/a-project")
                                                      .cloneUrlSsh("git@gitlab.com:gitlab-clone-example/a-project.git")
                                                      .build();

        final Git repo = new GitService().clone(repository, cloneDirectory.toPath().toString(), false);

        assertThat(repo.log().call()).isNotEmpty();
        assertThat(repo.submoduleStatus().call()).containsKey("some-project-sub-module")
                                                 .allSatisfy(this::submoduleIsUninitialized);
    }

    @Test
    void testClonePublicRepositories_ssh_freshClone_withSubmodules() throws GitAPIException {
        final GitService gitService = new GitService();
        Flux<GitRepository> repositories = Flux.just(
                GitRepository.builder()
                             .name("gitlab-clone-example / a-project")
                             .path("gitlab-clone-example/a-project")
                             .cloneUrlSsh("git@gitlab.com:gitlab-clone-example/a-project.git")
                             .build(),
                GitRepository.builder()
                             .name("gitlab-clone-example / sub-group-1 / some-project")
                             .path("gitlab-clone-example/sub-group-1/some-project")
                             .cloneUrlSsh("git@gitlab.com:gitlab-clone-example/sub-group-1/some-project.git")
                             .build(),
                GitRepository.builder()
                             .name("gitlab-clone-example / sub-group-2 / sub-group-3 / another-project")
                             .path("gitlab-clone-example/sub-group-2/sub-group-3/another-project")
                             .cloneUrlSsh("git@gitlab.com:gitlab-clone-example/sub-group-2/sub-group-3/another-project.git")
                             .build()
        );

        final Stream<Either<Throwable, Git>> result = FluxToStream(repositories.map(repository -> gitService.clone(repository, cloneDirectoryPath)));
        final List<Git> gits = result.filter(Either::isRight).map(Either::get).toJavaList();

        assertThat(gits).hasSize(3);
        assertThat(gits.get(0).submoduleStatus().call()).containsKey("some-project-sub-module")
                                                        .allSatisfy(this::submoduleIsUninitialized);
    }

    @Test
    void testCloneOrInitSubmodulesPublicRepos_ssh_existingClone_withSubmodules() throws GitAPIException {
        final GitService gitService = new GitService();
        Flux<GitRepository> repositories = Flux.just(
                GitRepository.builder()
                             .name("gitlab-clone-example / a-project")
                             .path("gitlab-clone-example/a-project")
                             .cloneUrlSsh("git@gitlab.com:gitlab-clone-example/a-project.git")
                             .build(),
                GitRepository.builder()
                             .name("gitlab-clone-example / sub-group-1 / some-project")
                             .path("gitlab-clone-example/sub-group-1/some-project")
                             .cloneUrlSsh("git@gitlab.com:gitlab-clone-example/sub-group-1/some-project.git")
                             .build(),
                GitRepository.builder()
                             .name("gitlab-clone-example / sub-group-2 / sub-group-3 / another-project")
                             .path("gitlab-clone-example/sub-group-2/sub-group-3/another-project")
                             .cloneUrlSsh("git@gitlab.com:gitlab-clone-example/sub-group-2/sub-group-3/another-project.git")
                             .build()
        );
        // create first clone with only one repo, without submodules
        final GitRepository firstRepository = repositories.blockFirst();
        final Git existingClone = gitService.clone(firstRepository, cloneDirectoryPath, true);
        assertThat(existingClone).isNotNull();
        assertThat(existingClone.submoduleStatus().call()).containsKey("some-project-sub-module")
                                                          .allSatisfy(this::submoduleIsInitialized);

        // clone entire group
        final Stream<Either<Throwable, Git>> result = FluxToStream(repositories.map(repository -> gitService.cloneOrInitSubmodules(repository, cloneDirectoryPath)));
        final List<Throwable> errors = result.filter(Either::isLeft).map(Either::getLeft).toJavaList();
        final List<Git> gits = result.filter(Either::isRight).map(Either::get).toJavaList();
        final java.util.stream.Stream<Map<String, SubmoduleStatus>> submoduleStatus = gits.stream().map(git -> {
            try {
                return git.submoduleStatus().call();
            } catch (GitAPIException e) {
                e.printStackTrace();
                return Map.of();
            }
        });

        assertThat(errors).isEmpty();
        assertThat(gits).hasSize(3);
        assertThat(submoduleStatus).allSatisfy(subModules -> assertThat(subModules).allSatisfy(this::submoduleIsInitialized));
    }

    @Test
    void testCloneOrInitSubmodulesPublicRepos_ssh_existingClone_withoutSubmodules() throws GitAPIException {
        final GitService gitService = new GitService();
        Flux<GitRepository> repositories = Flux.just(
                GitRepository.builder()
                             .name("gitlab-clone-example / a-project")
                             .path("gitlab-clone-example/a-project")
                             .cloneUrlSsh("git@gitlab.com:gitlab-clone-example/a-project.git")
                             .build(),
                GitRepository.builder()
                             .name("gitlab-clone-example / sub-group-1 / some-project")
                             .path("gitlab-clone-example/sub-group-1/some-project")
                             .cloneUrlSsh("git@gitlab.com:gitlab-clone-example/sub-group-1/some-project.git")
                             .build(),
                GitRepository.builder()
                             .name("gitlab-clone-example / sub-group-2 / sub-group-3 / another-project")
                             .path("gitlab-clone-example/sub-group-2/sub-group-3/another-project")
                             .cloneUrlSsh("git@gitlab.com:gitlab-clone-example/sub-group-2/sub-group-3/another-project.git")
                             .build()
        );
        // create first clone with only one repo
        final GitRepository firstRepository = repositories.blockFirst();
        final Git existingClone = gitService.clone(firstRepository, cloneDirectoryPath, false);
        assertThat(existingClone).isNotNull();
        assertThat(existingClone.submoduleStatus().call()).containsKey("some-project-sub-module")
                                                          .allSatisfy(this::submoduleIsUninitialized);

        // clone entire group
        final Stream<Either<Throwable, Git>> result = FluxToStream(repositories.map(repository -> gitService.cloneOrInitSubmodules(repository, cloneDirectoryPath)));
        final List<Throwable> errors = result.filter(Either::isLeft).map(Either::getLeft).toJavaList();
        final List<Git> gits = result.filter(Either::isRight).map(Either::get).toJavaList();
        final java.util.stream.Stream<Map<String, SubmoduleStatus>> submoduleStatus = gits.stream().map(git -> {
            try {
                return git.submoduleStatus().call();
            } catch (GitAPIException e) {
                e.printStackTrace();
                return Map.of();
            }
        });

        assertThat(errors).isEmpty();
        assertThat(gits).hasSize(3);
        assertThat(submoduleStatus).allSatisfy(subModules -> assertThat(subModules).allSatisfy(this::submoduleIsInitialized));
    }

    private <T> Stream<T> FluxToStream(Flux<T> gits) {
        return Stream.ofAll(gits.collectList().block());
    }

    private void submoduleIsInitialized(String name, SubmoduleStatus status) {
        assertSubmoduleStatus(name, status, INITIALIZED);
    }

    private void submoduleIsUninitialized(String name, SubmoduleStatus value) {
        assertSubmoduleStatus(name, value, UNINITIALIZED);
    }

    private void assertSubmoduleStatus(String name, SubmoduleStatus status, SubmoduleStatusType initialized) {
        assertThat(status)
                .extracting("type")
                .as("Check status of submodule " + name)
                .isInstanceOfSatisfying(SubmoduleStatusType.class, statusType -> assertThat(statusType).isEqualTo(initialized));
    }
}
