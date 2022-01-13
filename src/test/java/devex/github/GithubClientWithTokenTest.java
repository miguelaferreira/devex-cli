package devex.github;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class GithubClientWithTokenTest {

    @Inject
    private GithubClient client;

    @Test
    void getOrganization() {
        final Optional<GithubOrganization> maybeOrg = client.getOrganization("devex-cli-example");

        assertThat(maybeOrg).isNotEmpty();
        assertThat(maybeOrg.get().getName()).isEqualTo("devex-cli-example");
    }

    @Test
    void getOrganizationRepositories() {
        final Flux<HttpResponse<List<GithubRepository>>> repositories = client.getOrganizationRepositories("devex-cli-example", 1);

        final Iterable<HttpResponse<List<GithubRepository>>> iterable = repositories.collectList().block();
        assertThat(iterable).hasSize(1);
        final HttpResponse<List<GithubRepository>> response = iterable.iterator().next();
        assertThat(response.getStatus().getCode()).isEqualTo(HttpStatus.OK.getCode());
        assertThat(response.getBody()).isNotEmpty();
        assertThat(response.getBody().get()).hasSize(3)
                                            .allSatisfy(repository -> assertThat(repository.getFullName()).containsIgnoringCase("devex-cli-example"));
    }
}
