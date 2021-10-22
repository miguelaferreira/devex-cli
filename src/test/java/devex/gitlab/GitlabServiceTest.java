package devex.gitlab;

import devex.TestBase;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.vavr.control.Either;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.assertj.vavr.api.VavrAssertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class GitlabServiceTest extends TestBase {

    public static final String GITLAB_GROUP_NAME = "gitlab-clone-example";
    public static final String GITLAB_GROUP_FULL_PATH = "gitlab-clone-example/sub-group-2/sub-group-3";
    public static final String GITLAB_GROUP_ID = "11961707";

    @Inject
    private GitlabService service;

    @Test
    void findGroupById() {
        final Either<String, GitlabGroup> maybeGroup = service.findGroupBy(GITLAB_GROUP_ID, GitlabGroupSearchMode.ID);

        VavrAssertions.assertThat(maybeGroup).isRight();
        Assertions.assertThat(maybeGroup.get().getName()).isEqualTo(GITLAB_GROUP_NAME);
    }

    @Test
    void findGroupByName() {
        final Either<String, GitlabGroup> maybeGroup = service.findGroupBy(GITLAB_GROUP_NAME, GitlabGroupSearchMode.NAME);

        VavrAssertions.assertThat(maybeGroup).isRight();
        Assertions.assertThat(maybeGroup.get().getName()).isEqualTo(GITLAB_GROUP_NAME);
    }

    @Test
    void findGroupByFullPath() {
        final Either<String, GitlabGroup> maybeGroup = service.findGroupBy(GITLAB_GROUP_FULL_PATH, GitlabGroupSearchMode.FULL_PATH);

        VavrAssertions.assertThat(maybeGroup).isRight();
        Assertions.assertThat(maybeGroup.get().getName()).isEqualTo("sub-group-3");
    }

    @Test
    void getDescendantGroups() {
        final Flux<GitlabGroup> descendantGroups = service.getDescendantGroups(GITLAB_GROUP_ID);

        assertThat(descendantGroups.collectList().block()).hasSize(3);
    }

    @Test
    void getSubGroupsRecursively() {
        final Flux<GitlabGroup> descendantGroups = service.getSubGroupsRecursively(GITLAB_GROUP_ID);

        assertThat(descendantGroups.collectList().block()).hasSize(3);
    }

    @Test
    void getGitlabGroupProjects() {
        final GitlabGroup group = service.findGroupBy(GITLAB_GROUP_NAME, GitlabGroupSearchMode.NAME).get();
        final Flux<GitlabProject> projects = service.getGitlabGroupProjects(group);

        assertThat(projects.collectList().block()).hasSize(3);
    }
}
