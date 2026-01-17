package com.requillion.solutions.inventory.util;

import com.requillion.solutions.inventory.security.RequestContext;
import com.requillion.solutions.inventory.security.UserContext;
import org.slf4j.Logger;

public class LoggerUtil {

    private static String formatMessage(String message) {
        RequestContext context = UserContext.getContext();
        if (context != null && context.getRequestId() != null) {
            return String.format("[%s] %s", context.getRequestId(), message);
        }
        return String.format("[NO_CONTEXT] %s", message);
    }

    private static String formatMessage(String format, Object... args) {
        return formatMessage(String.format(format, args));
    }

    public static void trace(Logger logger, String format, Object... args) {
        if (logger.isTraceEnabled()) {
            logger.trace(formatMessage(format, args));
        }
    }

    public static void debug(Logger logger, String format, Object... args) {
        if (logger.isDebugEnabled()) {
            logger.debug(formatMessage(format, args));
        }
    }

    public static void info(Logger logger, String format, Object... args) {
        if (logger.isInfoEnabled()) {
            logger.info(formatMessage(format, args));
        }
    }

    public static void warn(Logger logger, String format, Object... args) {
        if (logger.isWarnEnabled()) {
            logger.warn(formatMessage(format, args));
        }
    }

    public static void error(Logger logger, String format, Object... args) {
        if (logger.isErrorEnabled()) {
            logger.error(formatMessage(format, args));
        }
    }

    public static void error(Logger logger, String message, Throwable throwable) {
        if (logger.isErrorEnabled()) {
            logger.error(formatMessage(message), throwable);
        }
    }
}
