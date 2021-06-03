package devex.gitlab;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Flowable;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class GitlabClientWithTokenTest extends GitlabTestBase {

    public static final String PRIVATE_GROUP_ID = "12040044";
    public static final String PUBLIC_GROUP_ID = "11961707";
    public static final String PRIVATE_GROUP_NAME = "gitlab-clone-example-private";
    public static final String PUBLIC_GROUP_NAME = "gitlab-clone-example";

    @Inject
    private GitlabClient client;

    @Test
    void searchGroups_privateGroup() {
        final Flowable<HttpResponse<List<GitlabGroup>>> groups = client.searchGroups(PRIVATE_GROUP_NAME, true, 10, 1);

        final Iterable<HttpResponse<List<GitlabGroup>>> iterable = groups.blockingIterable();
        assertThat(iterable).hasSize(1);
        final HttpResponse<List<GitlabGroup>> response = iterable.iterator().next();
        assertThat(response.getStatus().getCode()).isEqualTo(HttpStatus.OK.getCode());
        assertThat(response.getBody()).isNotEmpty();
        assertThat(response.getBody().get()).hasSize(2)
                                            .allSatisfy(group -> assertThat(group.getFullPath()).contains(PRIVATE_GROUP_NAME));
    }

    @Test
    void getGroup_privateGroup_withoutToken() {
        final Optional<GitlabGroup> maybeGroup = client.getGroup(PRIVATE_GROUP_ID);

        assertThat(maybeGroup).isNotEmpty();
        assertThat(maybeGroup.get().getId()).isEqualTo(PRIVATE_GROUP_ID);
        assertThat(maybeGroup.get().getName()).isEqualTo(PRIVATE_GROUP_NAME);
    }

    @Test
    void searchGroups_publicGroup() {
        final Flowable<HttpResponse<List<GitlabGroup>>> groups = client.searchGroups(PUBLIC_GROUP_NAME, true, 10, 1);

        final Iterable<HttpResponse<List<GitlabGroup>>> iterable = groups.blockingIterable();
        assertThat(iterable).hasSize(1);
        final HttpResponse<List<GitlabGroup>> response = iterable.iterator().next();
        assertThat(response.getStatus().getCode()).isEqualTo(HttpStatus.OK.getCode());
        assertThat(response.getBody()).isNotEmpty();
        assertThat(response.getBody().get()).hasSize(6)
                                            .allSatisfy(group -> assertThat(group.getFullPath()).contains(PUBLIC_GROUP_NAME));
    }

    @Test
    void groupSubGroups_privateGroup() {
        final Flowable<HttpResponse<List<GitlabGroup>>> groups = client.groupSubGroups(PRIVATE_GROUP_NAME, true, 10, 1);

        final Iterable<HttpResponse<List<GitlabGroup>>> iterable = groups.blockingIterable();
        assertThat(iterable).hasSize(1);
        final HttpResponse<List<GitlabGroup>> response = iterable.iterator().next();
        assertThat(response.getStatus().getCode()).isEqualTo(HttpStatus.OK.getCode());
        assertThat(response.getBody()).isNotEmpty();
        assertThat(response.getBody().get()).hasSize(1)
                                            .allSatisfy(group -> assertThat(group.getFullPath()).contains(PRIVATE_GROUP_NAME));
    }

    @Test
    void groupSubGroups_publicGroup() {
        final Flowable<HttpResponse<List<GitlabGroup>>> groups = client.groupSubGroups(PUBLIC_GROUP_ID, true, 10, 1);

        final Iterable<HttpResponse<List<GitlabGroup>>> iterable = groups.blockingIterable();
        assertThat(iterable).hasSize(1);
        final HttpResponse<List<GitlabGroup>> response = iterable.iterator().next();
        assertThat(response.getStatus().getCode()).isEqualTo(HttpStatus.OK.getCode());
        assertThat(response.getBody()).isNotEmpty();
        assertThat(response.getBody().get()).hasSize(2)
                                            .allSatisfy(group -> assertThat(group.getFullPath()).contains(PUBLIC_GROUP_NAME));
    }

    @Test
    void groupDescendants_privateGroup() {
        final Flowable<HttpResponse<List<GitlabGroup>>> groups = client.groupDescendants(PRIVATE_GROUP_ID, true, 10, 1);

        final Iterable<HttpResponse<List<GitlabGroup>>> iterable = groups.blockingIterable();
        assertThat(iterable).hasSize(1);
        final HttpResponse<List<GitlabGroup>> response = iterable.iterator().next();
        assertThat(response.getStatus().getCode()).isEqualTo(HttpStatus.OK.getCode());
        assertThat(response.getBody()).isNotEmpty();
        assertThat(response.getBody().get()).hasSize(1)
                                            .allSatisfy(group -> assertThat(group.getFullPath()).contains(PRIVATE_GROUP_NAME));
    }

    @Test
    void groupDescendants_publicGroup() {
        final Flowable<HttpResponse<List<GitlabGroup>>> groups = client.groupDescendants(PUBLIC_GROUP_ID, true, 10, 1);

        final Iterable<HttpResponse<List<GitlabGroup>>> iterable = groups.blockingIterable();
        assertThat(iterable).hasSize(1);
        final HttpResponse<List<GitlabGroup>> response = iterable.iterator().next();
        assertThat(response.getStatus().getCode()).isEqualTo(HttpStatus.OK.getCode());
        assertThat(response.getBody()).isNotEmpty();
        assertThat(response.getBody().get()).hasSize(3)
                                            .allSatisfy(group -> assertThat(group.getFullPath()).contains(PUBLIC_GROUP_NAME));
    }

    @Test
    void groupProjects_publicGroup() {
        final Flowable<HttpResponse<List<GitlabProject>>> groups = client.groupProjects(PUBLIC_GROUP_ID, true, 10, 1);

        final Iterable<HttpResponse<List<GitlabProject>>> iterable = groups.blockingIterable();
        assertThat(iterable).hasSize(1);
        final HttpResponse<List<GitlabProject>> response = iterable.iterator().next();
        assertThat(response.getStatus().getCode()).isEqualTo(HttpStatus.OK.getCode());
        assertThat(response.getBody()).isNotEmpty();
        assertThat(response.getBody().get()).hasSize(1)
                                            .allSatisfy(project -> assertThat(project.getName()).isEqualTo("a-project"));
    }

    @Test
    void groupProjects_privateGroup() {
        final Flowable<HttpResponse<List<GitlabProject>>> groups = client.groupProjects(PRIVATE_GROUP_ID, true, 10, 1);

        final Iterable<HttpResponse<List<GitlabProject>>> iterable = groups.blockingIterable();
        assertThat(iterable).hasSize(1);
        final HttpResponse<List<GitlabProject>> response = iterable.iterator().next();
        assertThat(response.getStatus().getCode()).isEqualTo(HttpStatus.OK.getCode());
        assertThat(response.getBody()).isNotEmpty();
        assertThat(response.getBody().get()).hasSize(1)
                                            .allSatisfy(project -> assertThat(project.getName()).isEqualTo("a-private-project"));
    }

    @Test
    void getVersion() {
        final GitlabVersion version = client.version();

        assertThat(version).isNotNull();
        assertThat(version.getVersion()).isNotNull();
    }
}
