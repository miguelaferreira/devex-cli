package devex.github;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.vavr.control.Either;
import reactor.core.publisher.Flux;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class GithubServiceTest {

    private static final String ORGANIZATION = "kubernetes";
    private static final String USER = "miguelaferreira";

    @Inject
    private GithubService service;

    @Test
    @Property(
            name = "github.token" // clear the property
    )
    void getOrganizationRepositories() {
        try {
            final Flux<GithubRepository> repositories = service.getOrganizationRepositories(ORGANIZATION);

            final Iterable<GithubRepository> iterable = repositories.toIterable();
            assertThat(iterable).hasSizeGreaterThan(50);
        } catch (HttpClientResponseException e) {
            GithubTestHelper.handleException(e);
        }
    }

    @Test
    void getUser() {
        // No @Property to clear the token: getUser swallows HTTP errors into a Left,
        // so an unauthenticated rate-limited response is indistinguishable from a real
        // "not found". Running authenticated (when GITHUB_TOKEN is set) keeps it
        // deterministic, while the guard below still tolerates a tokenless CI run.
        final Either<String, GithubOrganization> maybeUser = service.getUser(USER);

        // getUser wraps HTTP errors into a Left, so a rate-limited response surfaces
        // here rather than as a thrown exception; skip the test in that case.
        if (maybeUser.isLeft() && GithubTestHelper.isRateLimitExceeded(maybeUser.getLeft())) {
            System.out.println(GithubTestHelper.RATE_LIMIT_SKIP_MESSAGE);
            return;
        }

        assertThat(maybeUser.isRight()).as("user '%s' should be found", USER).isTrue();
        assertThat(maybeUser.get().getName()).isEqualTo(USER);
    }

    @Test
    @Property(
            name = "github.token" // clear the property
    )
    void getUserRepositories() {
        try {
            final Flux<GithubRepository> repositories = service.getUserRepositories(USER);

            final Iterable<GithubRepository> iterable = repositories.toIterable();
            assertThat(iterable).isNotEmpty()
                                .allSatisfy(repository -> assertThat(repository.getFullName()).containsIgnoringCase(USER));
        } catch (HttpClientResponseException e) {
            GithubTestHelper.handleException(e);
        }
    }
}
