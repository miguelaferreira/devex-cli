package devex.github;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class GithubServiceTest {

    @Inject
    private GithubService service;

    @Test
    @Property(
            name = "github.token" // clear the property
    )
    void getOrganizationRepositories() {
        try {
            final Flux<GithubRepository> repositories = service.getOrganizationRepositories("kubernetes");

            final Iterable<GithubRepository> iterable = repositories.collectList().block();
            assertThat(iterable).hasSize(74);
        } catch (HttpClientResponseException e) {
            GithubTestHelper.handleException(e);
        }
    }
}
