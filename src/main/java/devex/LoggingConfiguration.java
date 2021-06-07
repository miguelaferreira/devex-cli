package devex;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import io.micronaut.logging.LogLevel;
import io.micronaut.logging.LoggingSystem;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class LoggingConfiguration {

    public static final String APPLICATION_LOGGER = "devex";

    public static final String[] ALL_LOGGERS = {
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
    };

    public static void configureLoggers(LoggingSystem loggingSystem, LogLevel level, boolean fullLogs) throws JoranException {
        if (fullLogs) {
            loadFullLogsConfig();
            configureAllLoggers(loggingSystem, level);
        } else {
            configureApplicationLoggers(loggingSystem, level);
        }
    }

    private static void configureAllLoggers(LoggingSystem loggingSystem, LogLevel level) {
        Arrays.stream(ALL_LOGGERS).forEach(logger -> loggingSystem.setLogLevel(logger, level));
    }

    public static void configureApplicationLoggers(LoggingSystem loggingSystem, LogLevel level) {
        loggingSystem.setLogLevel(APPLICATION_LOGGER, level);
    }

    public static void loadLogsConfig() throws JoranException {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        configurator.doConfigure(LoggingConfiguration.class.getResourceAsStream("/logback.xml"));
    }

    public static void loadFullLogsConfig() throws JoranException {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.reset();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        configurator.doConfigure(LoggingConfiguration.class.getResourceAsStream("/logback-full-logs.xml"));
    }
}
