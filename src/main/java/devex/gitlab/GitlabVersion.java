package devex.gitlab;

import tools.jackson.core.Version;
import tools.jackson.core.util.VersionUtil;
import io.micronaut.serde.annotation.Serdeable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Serdeable
@Data
@AllArgsConstructor
public class GitlabVersion {
    String version;
    String revision;

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
