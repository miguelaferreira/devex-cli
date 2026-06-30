package devex.git;

import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.transport.sshd.OpenSshServerKeyDatabase;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.sshd.ServerKeyDatabase;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;

import net.i2p.crypto.eddsa.EdDSASecurityProvider;

import jakarta.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.security.Security;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Slf4j
@Singleton
public class GitService {

    static {
        // Apache MINA SSHD parses ssh-ed25519 keys (host keys and known_hosts entries)
        // by building them through java.security with the EdDSA provider. In the GraalVM
        // native image that provider isn't registered at run time, so every ssh-ed25519
        // known_hosts line is rejected as "invalid" and hosts that present an Ed25519 host
        // key (e.g. github.com) fall back to other algorithms and fail host-key validation.
        // Register it explicitly, before the session factory is built below (the factory
        // reads known_hosts lazily, but registering first keeps Ed25519 available from the
        // very first connection). Harmless on the JVM, where MINA registers it itself.
        if (Security.getProvider(EdDSASecurityProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new EdDSASecurityProvider());
        }
    }

    private static final SshSessionFactory sshSessionFactory = buildSshSessionFactory();

    static {
        // Submodule operations use the global default SshSessionFactory rather than
        // the per-transport callback, so install ours as the default.
        SshSessionFactory.setInstance(sshSessionFactory);
    }

    private static SshSessionFactory buildSshSessionFactory() {
        return new SshdSessionFactoryBuilder()
                .setHomeDirectory(new File(System.getProperty("user.home")))
                .setSshDirectory(Path.of(System.getProperty("user.home"), ".ssh").toFile())
                .setConfigFile(GitService::sshConfigFile)
                .setServerKeyDatabase(GitService::preferStrongHostKeys)
                .withDefaultConnectorFactory()
                .build(null);
    }

    // Rank host-key algorithms strongest-first; lower is preferred.
    private static int hostKeyAlgorithmRank(String algorithm) {
        final String algo = algorithm == null ? "" : algorithm.toLowerCase();
        if (algo.contains("eddsa") || algo.contains("ed25519")) {
            return 0;
        }
        if (algo.contains("ec")) { // ECDSA
            return 1;
        }
        if (algo.contains("rsa")) {
            return 2;
        }
        return 3;
    }

    // JGit proposes host-key algorithms in the order the server-key database returns
    // the known keys for a host, and the default database's order isn't stable. When a
    // host has several known_hosts entries of different types (e.g. github.com, for which
    // many users have both a current ssh-ed25519 entry and a stale pre-2023 ssh-rsa one),
    // a weaker/stale type can win negotiation and host-key validation fails with "Server
    // key did not validate". Sort the looked-up keys strongest-first so the proposal is
    // deterministic and prefers the strongest key the user actually trusts for that host
    // — mirroring OpenSSH, and without affecting hosts that only have one key type.
    private static ServerKeyDatabase preferStrongHostKeys(File homeDir, File sshDir) {
        final List<Path> knownHostsFiles = List.of(
                new File(sshDir, "known_hosts").toPath(),
                new File(sshDir, "known_hosts2").toPath());
        final ServerKeyDatabase delegate = new OpenSshServerKeyDatabase(true, knownHostsFiles);
        return new ServerKeyDatabase() {
            @Override
            public List<PublicKey> lookup(String connectAddress, InetSocketAddress remoteAddress, Configuration config) {
                final List<PublicKey> keys = new ArrayList<>(delegate.lookup(connectAddress, remoteAddress, config));
                keys.sort(Comparator.comparingInt(key -> hostKeyAlgorithmRank(key.getAlgorithm())));
                return keys;
            }

            @Override
            public boolean accept(String connectAddress, InetSocketAddress remoteAddress, PublicKey serverKey,
                                  Configuration config, CredentialsProvider provider) {
                return delegate.accept(connectAddress, remoteAddress, serverKey, config, provider);
            }
        };
    }

    static final String STRIPPED_LINE_PREFIX = "# (devex) stripped for MINA SSHD compatibility: ";

    // `IdentityFile <path>.pub` is OpenSSH's idiom for "ask the agent for the key
    // matching this public key" (used heavily with 1Password / hardware tokens).
    // MINA SSHD can't read it: it tries to parse the .pub as a private key and
    // bails, then — combined with `IdentitiesOnly yes` — refuses to fall back to
    // the agent. Strip both directives in a temp wrapper so the agent path stays
    // open while the rest of the user's config (IdentityAgent, HostName, etc.)
    // is honored unchanged.
    private static final Pattern IDENTITY_FILE_PUB = Pattern.compile(
            "^\\s*IdentityFile\\s+(?:\\S+\\.pub|\"[^\"]*\\.pub\")\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern IDENTITIES_ONLY_YES = Pattern.compile(
            "^\\s*IdentitiesOnly\\s+yes\\s*$",
            Pattern.CASE_INSENSITIVE);

    private static File sshConfigFile(File sshDir) {
        final File userConfig = new File(sshDir, "config");
        if (!userConfig.isFile()) {
            return userConfig;
        }
        try {
            final String original = Files.readString(userConfig.toPath());
            final String filtered = filterIncompatibleDirectives(original);
            if (filtered.equals(original)) {
                return userConfig;
            }
            final File tempConfig = Files.createTempFile("devex-ssh-config", ".cfg").toFile();
            tempConfig.deleteOnExit();
            Files.writeString(tempConfig.toPath(), filtered);
            return tempConfig;
        } catch (IOException e) {
            log.debug("Could not write filtered SSH config wrapper, falling back to user's config", e);
            return userConfig;
        }
    }

    static String filterIncompatibleDirectives(String content) {
        final String[] lines = content.split("\n", -1);
        final StringBuilder out = new StringBuilder(content.length() + lines.length);
        for (int i = 0; i < lines.length; i++) {
            final String line = lines[i];
            if (IDENTITY_FILE_PUB.matcher(line).matches() || IDENTITIES_ONLY_YES.matcher(line).matches()) {
                out.append(STRIPPED_LINE_PREFIX).append(line);
            } else {
                out.append(line);
            }
            if (i < lines.length - 1) {
                out.append('\n');
            }
        }
        return out.toString();
    }

    public static final String HTTPS_USERNAME = "git";

    private GitCloneProtocol cloneProtocol = GitCloneProtocol.SSH;
    private String httpsPassword = "";

    public void setCloneProtocol(GitCloneProtocol cloneProtocol) {
        this.cloneProtocol = cloneProtocol;
    }

    public void setHttpsPassword(String httpsPassword) {
        this.httpsPassword = httpsPassword;
    }

    public Either<Throwable, Git> cloneOrInitSubmodules(GitRepository repository, String cloneDirectory) {
        final String repositoryName = repository.getName();
        log.trace("Cloning or initializing submodules for repository '{}' under directory '{}'", repositoryName, cloneDirectory);
        return tryClone(repository, cloneDirectory, true)
                .recoverWith(t -> tryInitSubmodules(repository, cloneDirectory))
                .toEither();
    }

    public Either<Throwable, Git> clone(final GitRepository repository, final String cloneDirectory) {
        final String repositoryName = repository.getName();
        log.trace("Cloning repository '{}' under directory '{}'", repositoryName, cloneDirectory);
        if (isAlreadyCloned(repository, cloneDirectory)) {
            return Either.left(new RepositoryAlreadyClonedException(repositoryName));
        }
        return tryClone(repository, cloneDirectory, false)
                .toEither();
    }

    private boolean isAlreadyCloned(final GitRepository repository, final String cloneDirectory) {
        final String pathToRepo = cloneDirectory + FileSystems.getDefault().getSeparator() + repository.getPath();
        final File repositoryDirectory = new File(pathToRepo);
        if (!repositoryDirectory.isDirectory()) {
            return false;
        }
        return Try.withResources(() -> Git.open(repositoryDirectory))
                  .of(git -> true)
                  .getOrElse(false);
    }

    private Try<Git> tryClone(final GitRepository repository, final String cloneDirectory, final boolean cloneSubmodules) {
        String repositoryName = repository.getName();
        return Try.of(() -> clone(repository, cloneDirectory, cloneSubmodules))
                  .onSuccess(gitRepo -> log.trace("Cloned repository '{}' to '{}'", repositoryName, getDirectory(gitRepo)))
                  .onFailure(t -> logFailedClone(repositoryName, t));
    }

    private Try<Git> tryInitSubmodules(GitRepository repository, String cloneDirectory) {
        return Try.of(() -> initSubmodules(repository, cloneDirectory))
                  .onSuccess(gitRepo -> log.trace("Initialized submodules of git repository at '{}'", getDirectory(gitRepo)))
                  .onFailure(t2 -> logFailedSubmoduleInit(repository.getName(), t2));
    }

    protected Git openRepository(GitRepository repository, String cloneDirectory) throws IOException {
        String pathToRepo = cloneDirectory + FileSystems.getDefault().getSeparator() + repository.getPath();
        return Git.open(new File(pathToRepo));
    }

    private void logFailedClone(String repositoryName, Throwable throwable) {
        log.debug(String.format("Could not clone repository '%s' because: %s", repositoryName, throwable.getMessage()), throwable);
    }

    private String getDirectory(Git gitRepo) {
        return gitRepo.getRepository().getDirectory().toString();
    }

    protected Git clone(GitRepository repository, String cloneDirectory, boolean cloneSubmodules) throws GitAPIException {
        String pathToClone = cloneDirectory + FileSystems.getDefault().getSeparator() + repository.getPath();

        final CloneCommand cloneCommand = Git.cloneRepository();
        switch (cloneProtocol) {
            case SSH:
                cloneCommand.setURI(repository.getCloneUrlSsh());
                cloneCommand.setTransportConfigCallback(transport -> {
                    SshTransport sshTransport = (SshTransport) transport;
                    sshTransport.setSshSessionFactory(sshSessionFactory);
                });
                break;
            case HTTPS:
                cloneCommand.setURI(repository.getCloneUrlHttps());
                final String password = Objects.requireNonNullElse(httpsPassword, "");
                if (!password.isBlank()) {
                    cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(HTTPS_USERNAME, httpsPassword));
                } else {
                    log.debug("Credentials for HTTPS remote not set, repository to clone must be public.");
                }
                break;
        }
        cloneCommand.setDirectory(new File(pathToClone));
        cloneCommand.setCloneSubmodules(cloneSubmodules);
        cloneCommand.setCloneAllBranches(false);
        cloneCommand.setNoTags();

        return cloneCommand.call();
    }

    protected Git initSubmodules(GitRepository repository, String cloneDirectory) throws IOException, GitAPIException {
        final Git repo = openRepository(repository, cloneDirectory);
        repo.submoduleInit().call();
        repo.submoduleUpdate().call();
        return repo;
    }

    private void logFailedSubmoduleInit(String repositoryName, Throwable throwable) {
        log.debug(String.format("Could not initialize submodules for repository '%s' because: %s", repositoryName, throwable
                .getMessage()), throwable);
    }
}
