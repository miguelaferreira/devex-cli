package devex.github;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Flowable;
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

    @Inject
    private GithubClient client;

    @Test
    void getOrganization() {
        try {
            final Optional<GithubOrganization> maybeOrg = client.getOrganization("devex-cli-example");

            assertThat(maybeOrg).isNotEmpty();
            assertThat(maybeOrg.get().getName()).isEqualTo("devex-cli-example");
        } catch (HttpClientResponseException e) {
            GithubTestHelper.handleException(e);
        }
    }

    @Test
    void getOrganizationRepositories() {
        try {
            final Flowable<HttpResponse<List<GithubRepository>>> repositories = client.getOrganizationRepositories("devex-cli-example", 1);

            final Iterable<HttpResponse<List<GithubRepository>>> iterable = repositories.blockingIterable();
            assertThat(iterable).hasSize(1);
            final HttpResponse<List<GithubRepository>> response = iterable.iterator().next();
            assertThat(response.getStatus().getCode()).isEqualTo(HttpStatus.OK.getCode());
            assertThat(response.getBody()).isNotEmpty();
            assertThat(response.getBody().get()).hasSize(2)
                                                .allSatisfy(repository -> assertThat(repository.getFullName()).containsIgnoringCase("devex-cli-example"));
        } catch (HttpClientResponseException e) {
            GithubTestHelper.handleException(e);
        }
    }
}
