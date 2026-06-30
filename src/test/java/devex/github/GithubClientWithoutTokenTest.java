package devex.github;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import reactor.core.publisher.Flux;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
@Property(
        name = "github.token" // clear the property
)
class GithubClientWithoutTokenTest {

    private static final String ORGANIZATION = "devex-cli-example";
    private static final String USER = "miguelaferreira";

    @Inject
    private GithubClient client;

    @Test
    void getOrganization() {
        try {
            final Optional<GithubOrganization> maybeOrg = client.getOrganization(ORGANIZATION);

            assertThat(maybeOrg).isNotEmpty();
            assertThat(maybeOrg.get().getName()).isEqualTo(ORGANIZATION);
        } catch (HttpClientResponseException e) {
            GithubTestHelper.handleException(e);
        }
    }

    @Test
    void getOrganizationRepositories() {
        try {
            final Flux<HttpResponse<List<GithubRepository>>> repositories = client.getOrganizationRepositories(ORGANIZATION, 1);

            final Iterable<HttpResponse<List<GithubRepository>>> iterable = repositories.toIterable();
            assertThat(iterable).hasSize(1);
            final HttpResponse<List<GithubRepository>> response = iterable.iterator().next();
            assertThat(response.getStatus().getCode()).isEqualTo(HttpStatus.OK.getCode());
            assertThat(response.getBody()).isNotEmpty();
            assertThat(response.getBody().get()).isNotEmpty()
                                                .allSatisfy(repository -> assertThat(repository.getFullName()).containsIgnoringCase(ORGANIZATION));
        } catch (HttpClientResponseException e) {
            GithubTestHelper.handleException(e);
        }
    }

    @Test
    void getUser() {
        try {
            final Optional<GithubOrganization> maybeUser = client.getUser(USER);

            assertThat(maybeUser).isNotEmpty();
            assertThat(maybeUser.get().getName()).isEqualTo(USER);
        } catch (HttpClientResponseException e) {
            GithubTestHelper.handleException(e);
        }
    }

    @Test
    void getUserRepositories() {
        try {
            final Flux<HttpResponse<List<GithubRepository>>> repositories = client.getUserRepositories(USER, 1);

            final Iterable<HttpResponse<List<GithubRepository>>> iterable = repositories.toIterable();
            assertThat(iterable).hasSize(1);
            final HttpResponse<List<GithubRepository>> response = iterable.iterator().next();
            assertThat(response.getStatus().getCode()).isEqualTo(HttpStatus.OK.getCode());
            assertThat(response.getBody()).isNotEmpty();
            assertThat(response.getBody().get()).isNotEmpty()
                                                .allSatisfy(repository -> assertThat(repository.getFullName()).containsIgnoringCase(USER));
        } catch (HttpClientResponseException e) {
            GithubTestHelper.handleException(e);
        }
    }
}
