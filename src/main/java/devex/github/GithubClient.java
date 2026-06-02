package devex.github;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.retry.annotation.Retryable;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

@Retryable(excludes = HttpClientResponseException.class)
@Client("${github.url}")
@Header(name = GithubClient.H_PRIVATE_TOKEN, value = "token ${github.token:}")
@Header(name = "User-Agent", value = "DevEx Cli App")
public interface GithubClient {
    String H_PRIVATE_TOKEN = "Authorization";

    @Get(value = "/orgs/{org}")
    Optional<GithubOrganization> getOrganization(@PathVariable String org);

    @Get(value = "/orgs/{org}/repos{?page}")
    Flux<HttpResponse<List<GithubRepository>>> getOrganizationRepositories(@PathVariable String org, @QueryValue int page);
}
