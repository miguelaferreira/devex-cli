package devex.github;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.Flowable;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

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
            final Flowable<GithubRepository> repositories = service.getOrganizationRepositories("kubernetes");

            final Iterable<GithubRepository> iterable = repositories.blockingIterable();
            assertThat(iterable).hasSize(74);
        } catch (HttpClientResponseException e) {
            GithubTestHelper.handleException(e);
        }
    }
}
