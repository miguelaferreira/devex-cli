package devex.http;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.reactivex.Flowable;
import io.vavr.control.Option;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Slf4j
public class HttpClientUtils {

    public static <T> Flowable<T> paginatedApiCall(final Function<Integer, Flowable<HttpResponse<List<T>>>> apiCall, Function<HttpResponse<?>, String> nextPageExtractor) {
        log.trace("Invoking paginated API");
        final Flowable<HttpResponse<List<T>>> responses = callPage(apiCall, 1, nextPageExtractor);
        return responses.map(response -> Option.of(response.body()))
                        .filter(Option::isDefined)
                        .map(Option::get)
                        .flatMap(Flowable::fromIterable);
    }

    private static <T> Flowable<HttpResponse<T>> callPage(Function<Integer, Flowable<HttpResponse<T>>> apiCall, int pageIndex, Function<HttpResponse<?>, String> nextPageExtractor) {
        try {
            log.trace("Calling page {}", pageIndex);
            return apiCall.apply(pageIndex)
                          .flatMap(response -> {
                              final String nextPageHeader =
                                      Objects.requireNonNullElse(nextPageExtractor.apply(response), "0");
                              int nextPageIndex;
                              final Flowable<HttpResponse<T>> nextCall;
                              if (!nextPageHeader.isBlank() && (nextPageIndex = Integer.parseInt(nextPageHeader)) > 1) {
                                  log.trace("Next page is {}", nextPageIndex);
                                  nextCall = callPage(apiCall, nextPageIndex, nextPageExtractor);
                              } else {
                                  log.trace("No more pages");
                                  nextCall = Flowable.empty();
                              }
                              return Flowable.just(response).mergeWith(nextCall);
                          });
        } catch (HttpClientException e) {
            log.error("GitLab API call failed: {}", e.getMessage());
            return Flowable.empty();
        }
    }
}
