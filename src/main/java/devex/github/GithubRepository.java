package devex.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record GithubRepository(
        String id,
        String name,
        @JsonProperty("full_name") String fullName,
        @JsonProperty("ssh_url") String sshUrl,
        @JsonProperty("clone_url") String httpsUrl
) {
}
