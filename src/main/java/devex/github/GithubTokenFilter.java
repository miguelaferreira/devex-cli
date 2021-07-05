package devex.github;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MutableHttpHeaders;
import io.micronaut.http.MutableHttpRequest;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.ClientFilterChain;
import io.micronaut.http.filter.HttpClientFilter;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;

import java.util.Objects;

@Slf4j
@Filter(patterns = Filter.MATCH_ALL_PATTERN, serviceId = "${github.url}")
public class GithubTokenFilter implements HttpClientFilter {


    @Override
    public Publisher<? extends HttpResponse<?>> doFilter(MutableHttpRequest<?> request, ClientFilterChain chain) {
        final MutableHttpHeaders headers = request.getHeaders();
        if (headers.contains(GithubClient.H_PRIVATE_TOKEN) && tokenIsEmpty(headers)) {
            log.trace("Removing empty github auth header.");
            headers.remove(GithubClient.H_PRIVATE_TOKEN);
        }
        return chain.proceed(request);
    }

    private boolean tokenIsEmpty(MutableHttpHeaders headers) {
        return Objects.requireNonNull(headers.get(GithubClient.H_PRIVATE_TOKEN)).trim().equals("token");
    }
}
