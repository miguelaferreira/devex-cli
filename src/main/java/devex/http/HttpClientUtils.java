package devex.http;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientException;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Slf4j
public final class HttpClientUtils {

    private HttpClientUtils() {
    }

    public static <T> Flux<T> paginatedApiCall(final Function<Integer, Flux<HttpResponse<List<T>>>> apiCall, Function<HttpResponse<?>, String> nextPageExtractor) {
        log.trace("Invoking paginated API");
        final Flux<HttpResponse<List<T>>> responses = callPage(apiCall, 1, nextPageExtractor);
        return responses.mapNotNull(HttpResponse::body)
                        .flatMap(Flux::fromIterable);
    }

    private static <T> Flux<HttpResponse<T>> callPage(Function<Integer, Flux<HttpResponse<T>>> apiCall, int pageIndex, Function<HttpResponse<?>, String> nextPageExtractor) {
        try {
            log.trace("Calling page {}", pageIndex);
            return apiCall.apply(pageIndex)
                          .flatMap(response -> {
                              final String nextPageHeader =
                                      Objects.requireNonNullElse(nextPageExtractor.apply(response), "0");
                              final int nextPageIndex = nextPageHeader.isBlank() ? 0 : Integer.parseInt(nextPageHeader);
                              final Flux<HttpResponse<T>> nextCall;
                              if (nextPageIndex > 1) {
                                  log.trace("Next page is {}", nextPageIndex);
                                  nextCall = callPage(apiCall, nextPageIndex, nextPageExtractor);
                              } else {
                                  log.trace("No more pages");
                                  nextCall = Flux.empty();
                              }
                              return Flux.just(response).mergeWith(nextCall);
                          });
        } catch (HttpClientException e) {
            log.error("GitLab API call failed: {}", e.getMessage());
            return Flux.empty();
        }
    }
}
