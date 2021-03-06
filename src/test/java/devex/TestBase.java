package devex;

import ch.qos.logback.core.joran.spi.JoranException;
import org.junit.jupiter.api.BeforeAll;

public class TestBase {

    @BeforeAll
    static void beforeAll() throws JoranException {
        // Need to do this because configuration is static and loaded once per JVM,
        // if the full logs test runs before this one the full log configuration
        // remains active.
        LoggingConfiguration.loadLogsConfig();
    }

}
