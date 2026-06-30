package devex.github;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import reactor.core.publisher.Flux;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class GithubClientWithTokenTest {

    private static final String ORGANIZATION = "devex-cli-example";
    private static final String USER = "miguelaferreira";

    @Inject
    private GithubClient client;

    @Test
    void getOrganization() {
        final Optional<GithubOrganization> maybeOrg = client.getOrganization(ORGANIZATION);

        assertThat(maybeOrg).isNotEmpty();
        assertThat(maybeOrg.get().getName()).isEqualTo(ORGANIZATION);
    }

    @Test
    void getOrganizationRepositories() {
        final Flux<HttpResponse<List<GithubRepository>>> repositories = client.getOrganizationRepositories(ORGANIZATION, 1);

        final Iterable<HttpResponse<List<GithubRepository>>> iterable = repositories.toIterable();
        assertThat(iterable).hasSize(1);
        final HttpResponse<List<GithubRepository>> response = iterable.iterator().next();
        assertThat(response.getStatus().getCode()).isEqualTo(HttpStatus.OK.getCode());
        assertThat(response.getBody()).isNotEmpty();
        assertThat(response.getBody().get()).isNotEmpty()
                                            .allSatisfy(repository -> assertThat(repository.getFullName()).containsIgnoringCase(ORGANIZATION));
    }

    @Test
    void getUser() {
        final Optional<GithubOrganization> maybeUser = client.getUser(USER);

        assertThat(maybeUser).isNotEmpty();
        assertThat(maybeUser.get().getName()).isEqualTo(USER);
    }

    @Test
    void getUserRepositories() {
        final Flux<HttpResponse<List<GithubRepository>>> repositories = client.getUserRepositories(USER, 1);

        final Iterable<HttpResponse<List<GithubRepository>>> iterable = repositories.toIterable();
        assertThat(iterable).hasSize(1);
        final HttpResponse<List<GithubRepository>> response = iterable.iterator().next();
        assertThat(response.getStatus().getCode()).isEqualTo(HttpStatus.OK.getCode());
        assertThat(response.getBody()).isNotEmpty();
        assertThat(response.getBody().get()).isNotEmpty()
                                            .allSatisfy(repository -> assertThat(repository.getFullName()).containsIgnoringCase(USER));
    }
}
