package devex.github;

import io.micronaut.http.MutableHttpHeaders;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.ClientFilter;
import io.micronaut.http.annotation.RequestFilter;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * Removes the GitHub authorization header from requests when no token is configured,
 * so that public resources are accessed anonymously instead of with an empty token.
 */
@Slf4j
@ClientFilter(serviceId = "${github.url}")
public class GithubTokenFilter {

    @RequestFilter
    public void removeEmptyToken(MutableHttpRequest<?> request) {
        final MutableHttpHeaders headers = request.getHeaders();
        if (headers.contains(GithubClient.H_PRIVATE_TOKEN) && tokenIsEmpty(headers)) {
            log.trace("Removing empty github auth header.");
            headers.remove(GithubClient.H_PRIVATE_TOKEN);
        }
    }

    private boolean tokenIsEmpty(MutableHttpHeaders headers) {
        return Objects.requireNonNull(headers.get(GithubClient.H_PRIVATE_TOKEN)).trim().equals("token");
    }
}
