package devex.git;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GitRepository {
    String name;
    String path;
    String cloneUrlHttps;
    String cloneUrlSsh;
}
