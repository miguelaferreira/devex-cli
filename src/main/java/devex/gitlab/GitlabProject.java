package devex.gitlab;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record GitlabProject(
        String id,
        String name,
        @JsonProperty("name_with_namespace") String nameWithNamespace,
        String path,
        @JsonProperty("path_with_namespace") String pathWithNamespace,
        @JsonProperty("ssh_url_to_repo") String sshUrlToRepo,
        @JsonProperty("http_url_to_repo") String httpUrlToRepo
) {
}
