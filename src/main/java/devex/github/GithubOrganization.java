package devex.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.serde.annotation.Serdeable;

/**
 * A GitHub account that owns repositories: either an organization or a user.
 */
@Serdeable
public record GithubOrganization(
        @JsonProperty("login") String name,
        String id,
        @JsonProperty("repos_url") String reposUrl
) {
}
