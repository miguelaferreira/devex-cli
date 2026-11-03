package devex.git;

import lombok.Builder;

@Builder
public record GitRepository(String name, String path, String cloneUrlHttps, String cloneUrlSsh) {
}
