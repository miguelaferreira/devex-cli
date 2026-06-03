package devex.git;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GitServiceConfigFilterTest {

    private static final String STRIPPED = GitService.STRIPPED_LINE_PREFIX;

    @Test
    void stripsIdentityFilePointingAtPublicKey() {
        final String input = """
                Host gitlab.com
                  IdentityFile ~/.ssh/id_ed25519.pub
                """;

        final String output = GitService.filterIncompatibleDirectives(input);

        assertThat(output).contains(STRIPPED + "  IdentityFile ~/.ssh/id_ed25519.pub");
        assertThat(output).doesNotContain("\n  IdentityFile ~/.ssh/id_ed25519.pub");
    }

    @Test
    void stripsIdentitiesOnlyYes() {
        final String input = """
                Host gitlab.com
                  IdentitiesOnly yes
                """;

        final String output = GitService.filterIncompatibleDirectives(input);

        assertThat(output).contains(STRIPPED + "  IdentitiesOnly yes");
        assertThat(output).doesNotContain("\n  IdentitiesOnly yes");
    }

    @Test
    void preservesIdentityFilePointingAtPrivateKey() {
        final String input = """
                Host hopper
                  IdentityFile ~/.ssh/id_rsa
                """;

        final String output = GitService.filterIncompatibleDirectives(input);

        assertThat(output).isEqualTo(input);
    }

    @Test
    void preservesIdentitiesOnlyNo() {
        final String input = """
                Host *
                  IdentitiesOnly no
                """;

        final String output = GitService.filterIncompatibleDirectives(input);

        assertThat(output).isEqualTo(input);
    }

    @Test
    void isCaseInsensitiveAcrossDirectiveAndValue() {
        final String input = """
                Host x
                  identityfile ~/.ssh/key.PUB
                  IDENTITIESONLY YES
                """;

        final String output = GitService.filterIncompatibleDirectives(input);

        assertThat(output).contains(STRIPPED + "  identityfile ~/.ssh/key.PUB");
        assertThat(output).contains(STRIPPED + "  IDENTITIESONLY YES");
    }

    @Test
    void stripsQuotedPublicKeyPathWithSpaces() {
        final String input = """
                Host x
                  IdentityFile "~/key dir/id_ed25519.pub"
                """;

        final String output = GitService.filterIncompatibleDirectives(input);

        assertThat(output).contains(STRIPPED + "  IdentityFile \"~/key dir/id_ed25519.pub\"");
    }

    @Test
    void leavesUnrelatedDirectivesUntouched() {
        final String input = """
                # top of config
                Host gitlab.com
                  HostName gitlab.com
                  User git
                  Port 22
                  IdentityAgent ~/.1password/agent.sock

                Host *
                  ServerAliveInterval 60
                """;

        final String output = GitService.filterIncompatibleDirectives(input);

        assertThat(output).isEqualTo(input);
    }

    @Test
    void filtersOnlyMatchingLinesWhenMixed() {
        final String input = """
                Host gitlab.com
                  HostName gitlab.com
                  IdentityAgent ~/.1password/agent.sock
                  IdentityFile ~/.ssh/id_ed25519.pub
                  IdentitiesOnly yes
                """;

        final String output = GitService.filterIncompatibleDirectives(input);

        assertThat(output).contains("  HostName gitlab.com");
        assertThat(output).contains("  IdentityAgent ~/.1password/agent.sock");
        assertThat(output).contains(STRIPPED + "  IdentityFile ~/.ssh/id_ed25519.pub");
        assertThat(output).contains(STRIPPED + "  IdentitiesOnly yes");
    }

    @Test
    void preservesTrailingNewlineState() {
        final String withNewline = "Host x\n  IdentitiesOnly yes\n";
        assertThat(GitService.filterIncompatibleDirectives(withNewline)).endsWith("\n");

        final String withoutNewline = "Host x\n  IdentitiesOnly yes";
        assertThat(GitService.filterIncompatibleDirectives(withoutNewline)).doesNotEndWith("\n");
    }

    @Test
    void leavesEmptyConfigUnchanged() {
        assertThat(GitService.filterIncompatibleDirectives("")).isEmpty();
    }
}
