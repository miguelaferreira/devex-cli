package devex.gitlab;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.util.VersionUtil;
import io.micronaut.core.annotation.Introspected;
import lombok.AllArgsConstructor;
import lombok.Data;

@Introspected
@Data
@AllArgsConstructor
public class GitlabVersion {
    String version;
    String revision;

    @JsonCreator
    public GitlabVersion() {
    }

    public boolean isBefore(String lowerBound) {
        return compare(lowerBound) < 0;
    }

    int compare(String lowerBound) {
        final Version lowerBoundVersion = parse(lowerBound);
        final Version thisVersion = parse(version);

        return thisVersion.compareTo(lowerBoundVersion);
    }

    static Version parse(String text) {
        final String versionText;
        if (text.contains("-")) {
            final String[] mainSplit = text.split("-");
            versionText = mainSplit[0];
        } else {
            versionText = text;
        }
        return VersionUtil.parseVersion(versionText, null, null);
    }

    @Override
    public String toString() {
        return "v" + version + " at rev " + revision;
    }
}
