package devex.git;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.util.HashMap;
import java.util.Map;

class JSCHLogger implements com.jcraft.jsch.Logger {
    private final Map<Integer, Level> levels = new HashMap<>();

    private final Logger LOGGER;


    public JSCHLogger() {
        // Mapping between JSch levels and our own levels
        levels.put(com.jcraft.jsch.Logger.DEBUG, Level.TRACE);
        levels.put(com.jcraft.jsch.Logger.INFO, Level.DEBUG);
        levels.put(com.jcraft.jsch.Logger.WARN, Level.INFO);
        levels.put(com.jcraft.jsch.Logger.ERROR, Level.WARN);
        levels.put(com.jcraft.jsch.Logger.FATAL, Level.ERROR);

        LOGGER = LoggerFactory.getLogger(JSCHLogger.class);
    }

    @Override
    public boolean isEnabled(int pLevel) {
        return true; // here, all levels enabled
    }

    @Override
    public void log(int pLevel, String pMessage) {
        Level level = levels.get(pLevel);
        if (level == null) {
            level = Level.ERROR;
        }

        switch (level) {
            case TRACE -> LOGGER.trace(pMessage);
            case DEBUG -> LOGGER.debug(pMessage);
            case INFO -> LOGGER.info(pMessage);
            case WARN -> LOGGER.warn(pMessage);
            case ERROR -> LOGGER.error(pMessage);
        }
    }
}
