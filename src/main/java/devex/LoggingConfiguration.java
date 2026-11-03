package devex;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import io.micronaut.logging.LogLevel;
import io.micronaut.logging.LoggingSystem;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public final class LoggingConfiguration {

    public static final String APPLICATION_LOGGER = "devex";

    public static final List<String> ALL_LOGGERS = List.of(
            APPLICATION_LOGGER,
            "io.micronaut.context.env",
            "io.micronaut.http.client",
            "io.netty.handler.ssl",
            "org.eclipse.jgit.util",
            "org.eclipse.jgit.submodule",
            "org.eclipse.jgit.storage",
            "org.eclipse.jgit.gitrepo",
            "org.eclipse.jgit.events",
            "org.eclipse.jgit.api",
            "org.eclipse.jgit.errors",
            "org.eclipse.jgit.transport",
            "org.apache.sshd"
    );

    private static final String LOGS_CONFIG = "/logback.xml";
    private static final String FULL_LOGS_CONFIG = "/logback-full-logs.xml";

    private LoggingConfiguration() {
    }

    public static void configureLoggers(LoggingSystem loggingSystem, LogLevel level, boolean fullLogs) throws JoranException {
        if (fullLogs) {
            loadFullLogsConfig();
            configureAllLoggers(loggingSystem, level);
        } else {
            configureApplicationLoggers(loggingSystem, level);
        }
    }

    private static void configureAllLoggers(LoggingSystem loggingSystem, LogLevel level) {
        ALL_LOGGERS.forEach(logger -> loggingSystem.setLogLevel(logger, level));
    }

    public static void configureApplicationLoggers(LoggingSystem loggingSystem, LogLevel level) {
        loggingSystem.setLogLevel(APPLICATION_LOGGER, level);
    }

    public static void loadLogsConfig() throws JoranException {
        loadConfig(LOGS_CONFIG);
    }

    public static void loadFullLogsConfig() throws JoranException {
        loadConfig(FULL_LOGS_CONFIG);
    }

    private static void loadConfig(String resource) throws JoranException {
        final var context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();
        final var configurator = new JoranConfigurator();
        configurator.setContext(context);
        try (InputStream config = LoggingConfiguration.class.getResourceAsStream(resource)) {
            configurator.doConfigure(config);
        } catch (IOException e) {
            throw new JoranException("Could not read logging configuration " + resource, e);
        }
    }
}
