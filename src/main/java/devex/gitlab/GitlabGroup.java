package devex.gitlab;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record GitlabGroup(
        String id,
        String name,
        String path,
        @JsonProperty("full_path") String fullPath
) {
}
