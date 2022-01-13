package devex;

import ch.qos.logback.core.joran.spi.JoranException;
import io.micronaut.configuration.picocli.MicronautFactory;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.Environment;
import io.micronaut.logging.LogLevel;
import io.micronaut.logging.LoggingSystem;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@Slf4j
@Command(
        name = "devex",
        headerHeading = "Usage:%n%n",
        synopsisHeading = "%n",
        header = {
                "Developer experience tools, saving time by automating gruntwork."
        },
        descriptionHeading = "%nDescription:%n%n",
        description = {
                "The devex cli it a collection of tools aimed at automating day to day tasks performed by developers.",
                "The tools are grouped by the platform they work against, currently GitHub or GitLab.",
                "For each platform there is a different subcommand."
        },
        footer = {
                "%nCopyright(c) 2021 - Miguel Ferreira - GitHub/GitLab: @miguelaferreira"
        },
        parameterListHeading = "%nParameters:%n",
        optionListHeading = "%nOptions:%n",
        mixinStandardHelpOptions = true,
        versionProvider = DevexCommand.AppVersionProvider.class,
        sortOptions = false,
        usageHelpAutoWidth = true,
        subcommands = {GitlabCommand.class, GithubCommand.class, TerraformCommand.class},
        scope = CommandLine.ScopeType.INHERIT
)
public class DevexCommand {

    @Option(
            order = 10,
            names = {"-v", "--verbose"},
            description = "Print out extra information about what the tool is doing.",
            scope = CommandLine.ScopeType.INHERIT
    )
    private boolean verbose;

    @Option(
            order = 11,
            names = {"-x", "--very-verbose"},
            description = "Print out even more information about what the tool is doing.",
            scope = CommandLine.ScopeType.INHERIT
    )
    private boolean veryVerbose;

    @Option(
            order = 12,
            names = {"--debug"},
            description = "Sets all loggers to DEBUG level.",
            scope = CommandLine.ScopeType.INHERIT
    )
    private boolean debug;

    @Option(
            order = 13,
            names = {"--trace"},
            description = "Sets all loggers to TRACE level. WARNING: this setting will leak tokens used for HTTP authentication (eg. the GitLab token) to the logs, use with " +
                    "caution.",
            scope = CommandLine.ScopeType.INHERIT
    )
    private boolean trace;

    @Inject
    LoggingSystem loggingSystem;

    static int execute(String[] args) {
        final ApplicationContext ctx = ApplicationContext.builder(GitlabCloneCommand.class, Environment.CLI).start();
        return execute(ctx, args);
    }

    public static int execute(ApplicationContext ctx, String[] args) {
        try (ctx) {
            final DevexCommand app = ctx.getBean(DevexCommand.class);
            return new CommandLine(app, new MicronautFactory(ctx))
                    .setCaseInsensitiveEnumValuesAllowed(true)
                    .setAbbreviatedOptionsAllowed(true)
                    .setExecutionStrategy(app::executionStrategy)
                    .execute(args);
        }
    }

    public static void main(String[] args) {
        int exitCode = execute(args);
        System.exit(exitCode);
    }

    private int executionStrategy(CommandLine.ParseResult parseResult) {
        configureLogging();
        log.debug("devex {}", new AppVersionProvider().getVersionText());
        return new CommandLine.RunLast().execute(parseResult); // default execution strategy
    }

    private void configureLogging() {
        try {
            if (trace) {
                LoggingConfiguration.configureLoggers(loggingSystem, LogLevel.TRACE, true);
                log.trace("Set all loggers to TRACE");
            } else if (debug) {
                LoggingConfiguration.configureLoggers(loggingSystem, LogLevel.DEBUG, true);
                log.debug("Set all loggers to DEBUG");
            } else if (veryVerbose) {
                LoggingConfiguration.configureLoggers(loggingSystem, LogLevel.TRACE, false);
                log.trace("Set application loggers to TRACE.");
            } else if (verbose) {
                LoggingConfiguration.configureLoggers(loggingSystem, LogLevel.DEBUG, false);
                log.debug("Set application loggers to DEBUG.");
            } else {
                LoggingConfiguration.configureLoggers(loggingSystem, LogLevel.INFO, false);
            }
        } catch (JoranException e) {
            System.err.println("ERROR: failed to configure loggers.");
        }
    }

    static class AppVersionProvider implements CommandLine.IVersionProvider {

        public String getVersionText() {
            return String.join("", new AppVersionProvider().getVersion());
        }

        @Override
        public String[] getVersion() {
            final InputStream in = AppVersionProvider.class.getResourceAsStream("/VERSION");
            if (in != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String version = reader.lines().collect(Collectors.joining());
                return new String[]{"java: " + System.getProperty("java.version") + "\ndevex cli: v" + version};
            } else {
                return new String[]{"No version"};
            }
        }
    }
}
