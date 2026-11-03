package devex.terraform;

import org.slf4j.Logger;

public final class CommandUtils {

    private CommandUtils() {
    }

    public static String handleOperationException(final Throwable t, final String operationDescription, final Logger log) {
        log.debug("Exception :: {}", operationDescription, t);
        return t.getMessage();
    }
}
