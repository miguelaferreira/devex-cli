package devex.gitlab;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GitlabVersionTest extends GitlabTestBase {

    @Test
    void versionIsBefore_patchVersion() {
        final GitlabVersion version = new GitlabVersion("1.2.3", "");

        assertThat(version.isBefore("2"))
                .as("Check version is before")
                .isTrue();
        assertThat(version.isBefore("1.3"))
                .as("Check version is before")
                .isTrue();
        assertThat(version.isBefore("2.1"))
                .as("Check version is before")
                .isTrue();
        assertThat(version.isBefore("1.2.4"))
                .as("Check version is before")
                .isTrue();
        assertThat(version.isBefore("2.1.0"))
                .as("Check version is before")
                .isTrue();
    }

    @Test
    void versionIsNotBefore_patchVersion() {
        final GitlabVersion version = new GitlabVersion("1.2.3", "");

        assertThat(version.isBefore("1"))
                .as("Check version is not before")
                .isFalse();
        assertThat(version.isBefore("1.2"))
                .as("Check version is not before")
                .isFalse();
        assertThat(version.isBefore("1.2.0"))
                .as("Check version is not before")
                .isFalse();
        assertThat(version.isBefore("1.2.2"))
                .as("Check version is not before")
                .isFalse();
        assertThat(version.isBefore("1.2.3"))
                .as("Check version is not before")
                .isFalse();
    }

    @Test
    void versionIsBefore_minorVersion() {
        final GitlabVersion version = new GitlabVersion("1.2", "");

        assertThat(version.isBefore("2"))
                .as("Check version is before")
                .isTrue();
        assertThat(version.isBefore("1.2.1"))
                .as("Check version is before")
                .isTrue();
        assertThat(version.isBefore("1.3"))
                .as("Check version is before")
                .isTrue();
        assertThat(version.isBefore("2.1"))
                .as("Check version is before")
                .isTrue();
        assertThat(version.isBefore("2.1.0"))
                .as("Check version is before")
                .isTrue();
    }

    @Test
    void versionIsNotBefore_minorVersion() {
        final GitlabVersion version = new GitlabVersion("1.2", "");

        assertThat(version.isBefore("1"))
                .as("Check version is not before")
                .isFalse();
        assertThat(version.isBefore("1.2"))
                .as("Check version is not before")
                .isFalse();
        assertThat(version.isBefore("1.2.0"))
                .as("Check version is not before")
                .isFalse();
        assertThat(version.isBefore("1.2.0"))
                .as("Check version is not before")
                .isFalse();
    }

    @Test
    void versionIsBefore_majorVersion() {
        final GitlabVersion version = new GitlabVersion("1", "");

        assertThat(version.isBefore("1.1"))
                .as("Check version is before")
                .isTrue();
        assertThat(version.isBefore("2"))
                .as("Check version is before")
                .isTrue();
        assertThat(version.isBefore("2.0"))
                .as("Check version is before")
                .isTrue();
        assertThat(version.isBefore("2.0.0"))
                .as("Check version is before")
                .isTrue();
    }

    @Test
    void versionIsNotBefore_majorVersion() {
        final GitlabVersion version = new GitlabVersion("1", "");

        assertThat(version.isBefore("0"))
                .as("Check version is not before")
                .isFalse();
        assertThat(version.isBefore("1"))
                .as("Check version is not before")
                .isFalse();
        assertThat(version.isBefore("1.0"))
                .as("Check version is not before")
                .isFalse();
        assertThat(version.isBefore("0.1"))
                .as("Check version is not before")
                .isFalse();
        assertThat(version.isBefore("0.0.1"))
                .as("Check version is not before")
                .isFalse();
        assertThat(version.isBefore("1.0.0"))
                .as("Check version is not before")
                .isFalse();
    }
}
