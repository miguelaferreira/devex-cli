package devex.gitlab;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientException;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.reactivex.Flowable;
import io.vavr.control.Either;
import io.vavr.control.Option;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Slf4j
@Singleton
public class GitlabService {

    public static final int MAX_ELEMENTS_PER_PAGE = 10;
    public static final String RESPONSE_HEADER_NEXT_PAGE = "X-Next-Page";
    public static final String GROUP_DESCENDANTS_VERSION = "13.5";
    public static final String GROUP_NOT_FOUND = "Group not found";
    private final GitlabClient client;
    private final String gitlabUrl;

    public GitlabService(GitlabClient client, @Value("${gitlab.url}") String gitlabUrl) {
        this.client = client;
        this.gitlabUrl = gitlabUrl;
    }

    public Either<String, GitlabGroup> findGroupBy(String search, GitlabGroupSearchMode by) {
        log.debug("Looking for group {}: {}", by.textualQualifier(), search);
        if (by == GitlabGroupSearchMode.ID) {
            return getGroup(search);
        } else {
            Function<Integer, Flowable<HttpResponse<List<GitlabGroup>>>> apiCall = pageIndex -> client.searchGroups(search, true, MAX_ELEMENTS_PER_PAGE, pageIndex);
            final Flowable<GitlabGroup> results = paginatedApiCall(apiCall);
            return results.filter(gitlabGroup -> by.groupPredicate(search).test(gitlabGroup))
                          .map(Either::<String, GitlabGroup>right).blockingFirst(Either.left(GROUP_NOT_FOUND));
        }
    }

    public Either<String, GitlabGroup> getGroup(String id) {
        try {
            return Option.ofOptional(client.getGroup(id)).toEither(GROUP_NOT_FOUND);
        } catch (HttpClientResponseException e) {
            final HttpStatus status = e.getStatus();
            log.warn("Unexpected status {} fetching GitLab group {}: {}, ", status.getCode(), id, status.getReason());
            return Either.left(e.getMessage());
        }
    }

    public Flowable<GitlabProject> getGitlabGroupProjects(GitlabGroup group) {
        log.debug("Searching for projects in group '{}'", group.getFullPath());
        final String groupId = group.getId();
        final Flowable<GitlabProject> projects = getGroupProjects(groupId);
        final Flowable<GitlabGroup> subGroups = getSubGroups(groupId);
        return Flowable.mergeDelayError(projects, subGroups.flatMap(subGroup -> getGroupProjects(subGroup.getId())));
    }

    protected Flowable<GitlabGroup> getSubGroups(String groupId) {
        log.trace("Retrieving sub-groups of '{}'", groupId);
        final Option<GitlabVersion> maybeVersion = getVersion();
        if (maybeVersion.isDefined()) {
            final GitlabVersion gitlabVersion = maybeVersion.get();
            if (gitlabVersion.isBefore(GROUP_DESCENDANTS_VERSION)) {
                log.trace("Retrieving sib-groups recursively because GitLab server version is '{}'", gitlabVersion.getVersion());
                return getSubGroupsRecursively(groupId);
            }
        } else {
            log.trace("Could not get GitLab server version, defaulting to retrieving sub-groups with descendant API.");
        }
        return getDescendantGroups(groupId);
    }

    private Option<GitlabVersion> getVersion() {
        try {
            final GitlabVersion version = client.version();
            log.debug("GitLab server at '{}' is running version '{}'", gitlabUrl, version);
            return Option.of(version);
        } catch (HttpClientResponseException e) {
            final HttpStatus status = e.getStatus();
            if (status.equals(HttpStatus.UNAUTHORIZED)) {
                log.debug("Could not detect GitLab server version without a valid token.");
            } else {
                log.warn("Unexpected status {} checking GitLab version: {}, ", status.getCode(), status.getReason());
            }
        }
        return Option.none();
    }

    protected Flowable<GitlabGroup> getDescendantGroups(String groupId) {
        return paginatedApiCall(pageIndex -> client.groupDescendants(groupId, true, MAX_ELEMENTS_PER_PAGE, pageIndex));
    }

    protected Flowable<GitlabGroup> getSubGroupsRecursively(String groupId) {
        final Flowable<GitlabGroup> subGroups = paginatedApiCall(pageIndex -> client.groupSubGroups(groupId, true, MAX_ELEMENTS_PER_PAGE, pageIndex));
        return subGroups.flatMap(group -> Flowable.just(group).mergeWith(getSubGroupsRecursively(group.getId())));
    }

    private Flowable<GitlabProject> getGroupProjects(String groupId) {
        log.trace("Retrieving group '{}' projects", groupId);
        return paginatedApiCall(pageIndex -> client.groupProjects(groupId, true, MAX_ELEMENTS_PER_PAGE, pageIndex));
    }

    private <T> Flowable<T> paginatedApiCall(final Function<Integer, Flowable<HttpResponse<List<T>>>> apiCall) {
        log.trace("Invoking paginated API");
        final Flowable<HttpResponse<List<T>>> responses = callPage(apiCall, 1);
        return responses.map(response -> Option.of(response.body()))
                        .filter(Option::isDefined)
                        .map(Option::get)
                        .flatMap(Flowable::fromIterable);
    }

    private <T> Flowable<HttpResponse<T>> callPage(Function<Integer, Flowable<HttpResponse<T>>> apiCall, int pageIndex) {
        try {
            log.trace("Calling page {}", pageIndex);
            return apiCall.apply(pageIndex)
                          .flatMap(response -> {
                              final String nextPageHeader =
                                      Objects.requireNonNullElse(
                                              response.getHeaders().get(RESPONSE_HEADER_NEXT_PAGE),
                                              "0"
                                      );
                              int nextPageIndex;
                              final Flowable<HttpResponse<T>> nextCall;
                              if (!nextPageHeader.isBlank() && (nextPageIndex = Integer.parseInt(nextPageHeader)) > 1) {
                                  log.trace("Next page is {}", nextPageIndex);
                                  nextCall = callPage(apiCall, nextPageIndex);
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
