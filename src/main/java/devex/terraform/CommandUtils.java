package devex.terraform;

import java.util.function.Function;

import org.slf4j.Logger;

public class CommandUtils {

    public static String handleOperationException(final Throwable t, final String operationDescription, final Logger log) {
        log.debug("Exception :: " + operationDescription, t);
        return t.getMessage();
    }
}
