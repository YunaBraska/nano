package de.yuna.berlin.nativeapp.helper.logger.logic;

import de.yuna.berlin.nativeapp.helper.logger.model.LogErrorHandler;
import de.yuna.berlin.nativeapp.helper.logger.model.LogInfoHandler;
import de.yuna.berlin.nativeapp.helper.logger.model.LogLevel;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static de.yuna.berlin.nativeapp.helper.logger.model.LogLevel.DEBUG;
import static de.yuna.berlin.nativeapp.helper.logger.model.LogLevel.ERROR;
import static de.yuna.berlin.nativeapp.helper.logger.model.LogLevel.FATAL;
import static de.yuna.berlin.nativeapp.helper.logger.model.LogLevel.INFO;
import static de.yuna.berlin.nativeapp.helper.logger.model.LogLevel.TRACE;
import static de.yuna.berlin.nativeapp.helper.logger.model.LogLevel.WARN;
import static de.yuna.berlin.nativeapp.helper.logger.model.LogLevel.nanoLogLevelOf;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class NanoLogger {
    public static final Formatter DEFAULT_LOG_FORMATTER = new LogFormatterJson();
    public static final LogInfoHandler DEFAULT_LOG_INFO_HANDLER = new LogInfoHandler(DEFAULT_LOG_FORMATTER);
    public static final LogErrorHandler DEFAULT_LOG_ERROR_HANDLER = new LogErrorHandler(DEFAULT_LOG_FORMATTER);
    protected final Logger logger;
    protected LogQueue logQueue;
    public static AtomicInteger MAX_LOG_NAME_LENGTH = new AtomicInteger(10);

    public NanoLogger(final Object object) {
        this(object.getClass());
    }

    public NanoLogger(final Class<?> clazz) {
        logger = Logger.getLogger(clazz.getName());
        logger.setUseParentHandlers(false);
        addHandlerIfAbsent(DEFAULT_LOG_INFO_HANDLER);
        addHandlerIfAbsent(DEFAULT_LOG_ERROR_HANDLER);
        MAX_LOG_NAME_LENGTH.updateAndGet(length -> Math.max(length, clazz.getSimpleName().length()));
    }

    public Logger logger() {
        return logger;
    }

    public LogQueue logQueue() {
        return logQueue;
    }

    public synchronized NanoLogger logQueue(final LogQueue logQueue) {
        this.logQueue = logQueue;
        return this;
    }

    public Formatter formatter() {
        return logger.getHandlers().length > 0 ? logger.getHandlers()[0].getFormatter() : null;
    }

    public NanoLogger formatter(final Formatter formatter) {
        for (final Handler handler : logger.getHandlers()) {
            handler.setFormatter(formatter);
        }
        return this;
    }

    public NanoLogger level(final LogLevel level) {
        logger.setLevel(level.toJavaLogLevel());
        return this;
    }

    public LogLevel level() {
        return nanoLogLevelOf(logger.getLevel());
    }

    public NanoLogger fatal(final Supplier<String> message, final Object... params) {
        return log(FATAL, null, message, params);
    }

    public NanoLogger fatal(final Throwable thrown, final Supplier<String> message, final Object... params) {
        return log(FATAL, thrown, message, params);
    }

    public NanoLogger error(final Supplier<String> message, final Object... params) {
        return log(ERROR, null, message, params);
    }

    public NanoLogger error(final Throwable thrown, final Supplier<String> message, final Object... params) {
        return log(ERROR, thrown, message, params);
    }

    public NanoLogger warn(final Supplier<String> message, final Object... params) {
        return log(WARN, null, message, params);
    }

    public NanoLogger warn(final Throwable thrown, final Supplier<String> message, final Object... params) {
        return log(WARN, thrown, message, params);
    }

    public NanoLogger info(final Supplier<String> message, final Object... params) {
        return log(INFO, null, message, params);
    }

    public NanoLogger info(final Throwable thrown, final Supplier<String> message, final Object... params) {
        return log(INFO, thrown, message, params);
    }

    public NanoLogger debug(final Supplier<String> message, final Object... params) {
        return log(DEBUG, null, message, params);
    }

    public NanoLogger debug(final Throwable thrown, final Supplier<String> message, final Object... params) {
        return log(DEBUG, thrown, message, params);
    }

    public NanoLogger trace(final Supplier<String> message, final Object... params) {
        return log(TRACE, null, message, params);
    }

    public NanoLogger trace(final Throwable thrown, final Supplier<String> message, final Object... params) {
        return log(TRACE, thrown, message, params);
    }

    public NanoLogger log(final LogLevel level, final Supplier<String> message, final Object... params) {
        return log(level, null, message, params);
    }

    public NanoLogger log(final LogLevel level, final Throwable thrown, final Supplier<String> message, final Object... params) {
        if (level != null && message != null && logger.isLoggable(level.toJavaLogLevel())) {
            final LogRecord logRecord = new LogRecord(level.toJavaLogLevel(), message.get());
            logRecord.setParameters(params);
            logRecord.setThrown(thrown);
            logRecord.setLoggerName(logger.getName());
            // For internationalization
            // logRecord.setResourceBundle(javaLogger.getResourceBundle());
            // logRecord.setResourceBundleName(javaLogger.getResourceBundleName());
            if (logQueue == null || !logQueue.log(logger, logRecord)) {
                logger.log(logRecord);
            }
        }
        return this;
    }


    protected void addHandlerIfAbsent(final Handler newHandler) {
        for (final Handler existingHandler : logger.getHandlers()) {
            if (existingHandler.getClass().equals(newHandler.getClass())) {
                return;
            }
        }
        logger.addHandler(newHandler);
    }
}
