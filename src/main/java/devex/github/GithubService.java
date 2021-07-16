package devex.github;

import devex.http.HttpClientUtils;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.reactivex.Flowable;
import io.vavr.collection.Stream;
import io.vavr.control.Either;
import io.vavr.control.Option;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.net.URI;
import java.util.Arrays;

@Slf4j
@Singleton
public class GithubService {

    public static final String LINK_HEADER = "link";
    private final GithubClient client;

    public GithubService(GithubClient client) {
        this.client = client;
    }

    public Either<String, GithubOrganization> getOrganization(String organization) {
        log.debug("Getting organization {}", organization);
        try {
            return Option.ofOptional(client.getOrganization(organization))
                         .toEither("Organization not found");
        } catch (HttpClientResponseException e) {
            final HttpStatus status = e.getStatus();
            log.warn("Unexpected status {} fetching GitHub organization {}: {}, ",
                    status.getCode(),
                    organization, status.getReason());
            return Either.left(e.getMessage());
        }
    }

    public Flowable<GithubRepository> getOrganizationRepositories(String organization) {
        log.debug("Getting repositories of organization {}", organization);
        return HttpClientUtils.paginatedApiCall(pageIndex -> client.getOrganizationRepositories(organization, pageIndex), this::extractNextPageFromLinkHeader);
    }

    private String extractNextPageFromLinkHeader(HttpResponse<?> response) {
        // example header => link: <https://api.github.com/organizations/13629408/repos?page=2>; rel="next", <https://api.github.com/organizations/13629408/repos?page=3>; rel="last"
        final HttpHeaders headers = response.getHeaders();
        return Option.of(headers.get(LINK_HEADER))
                     // split all link entries
                     .map(header -> streamFromArray(header.split(",")))
                     // get next page link entry, example => <https://api.github.com/organizations/13629408/repos?page=2>; rel="next"
                     .map(stream -> stream.filter(element -> element.contains("rel=\"next\"")))
                     .flatMap(stream -> Option.of(stream.getOrNull()))
                     // split link entry and get first part (the link wrapped in '< >')
                     .map(nextPageEntry -> Arrays.asList(nextPageEntry.split(";")).get(0).trim())
                     // unwrap link
                     .map(wrappedLink -> wrappedLink.substring(1, wrappedLink.length() - 1))
                     // get query from link, example => ?page=2,something=else
                     .map(link -> URI.create(link).getQuery())
                     // split query parameters
                     .map(query -> streamFromArray(query.split(",")))
                     // get page parameter, example => page=2
                     .map(stream -> stream.filter(element -> element.contains("page")))
                     .flatMap(stream -> Option.of(stream.getOrNull()))
                     // split page parameter and get the page number
                     .map(parameter -> Arrays.asList(parameter.split("=")).get(1))
                     // if something went wrong short-circuit with 0 which will make pagination algorithm stop
                     .getOrElse("0");
    }

    private Stream<String> streamFromArray(String[] array) {
        return Stream.ofAll(Arrays.asList(array));
    }
}
