package devex.gitlab;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.retry.annotation.Retryable;
import io.reactivex.Flowable;

import java.util.List;
import java.util.Optional;

@Retryable(excludes = HttpClientResponseException.class)
@Client("${gitlab.url}/api/v4")
@Header(name = GitlabClient.H_PRIVATE_TOKEN, value = "${gitlab.token:}")
public interface GitlabClient {
    String H_PRIVATE_TOKEN = "PRIVATE-TOKEN";

    @Get("/groups{?search,per_page,all_available,page}")
    Flowable<HttpResponse<List<GitlabGroup>>> searchGroups(
            @QueryValue String search,
            @QueryValue(value = "all_available") boolean allAvailable,
            @QueryValue(value = "per_page") int perPage,
            @QueryValue int page
    );

    @Get("/groups/{id}")
    Optional<GitlabGroup> getGroup(@PathVariable String id);

    @Get("/groups/{id}/subgroups{?all_available,per_page,page}")
    Flowable<HttpResponse<List<GitlabGroup>>> groupSubGroups(
            @PathVariable String id,
            @QueryValue(value = "all_available") boolean allAvailable,
            @QueryValue(value = "per_page") int perPage,
            @QueryValue int page
    );

    @Get("/groups/{id}/descendant_groups{?all_available,per_page,page}")
    Flowable<HttpResponse<List<GitlabGroup>>> groupDescendants(
            @PathVariable String id,
            @QueryValue(value = "all_available") boolean allAvailable,
            @QueryValue(value = "per_page") int perPage,
            @QueryValue int page
    );

    @Get("/groups/{id}/projects")
    Flowable<HttpResponse<List<GitlabProject>>> groupProjects(
            @PathVariable String id,
            @QueryValue(value = "all_available") boolean allAvailable,
            @QueryValue(value = "per_page") int perPage,
            @QueryValue int page
    );

    @Get("/version")
    GitlabVersion version();
}
