package devex.gitlab;

import io.micronaut.serde.annotation.Serdeable;
import tools.jackson.core.Version;
import tools.jackson.core.util.VersionUtil;

@Serdeable
public record GitlabVersion(String version, String revision) {

    public boolean isBefore(String lowerBound) {
        return compare(lowerBound) < 0;
    }

    int compare(String lowerBound) {
        final Version lowerBoundVersion = parse(lowerBound);
        final Version thisVersion = parse(version);

        return thisVersion.compareTo(lowerBoundVersion);
    }

    static Version parse(String text) {
        final int suffixStart = text.indexOf('-');
        final String versionText = suffixStart < 0 ? text : text.substring(0, suffixStart);
        return VersionUtil.parseVersion(versionText, null, null);
    }

    @Override
    public String toString() {
        return "v" + version + " at rev " + revision;
    }
}
