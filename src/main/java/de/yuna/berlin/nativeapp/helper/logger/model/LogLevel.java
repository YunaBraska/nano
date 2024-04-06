package de.yuna.berlin.nativeapp.helper.logger.model;

import java.util.Arrays;
import java.util.logging.Level;

import static de.yuna.berlin.nativeapp.helper.NanoUtils.hasText;

public enum LogLevel {
    OFF(Level.OFF),
    FATAL(Level.SEVERE),
    ERROR(Level.SEVERE),
    WARN(Level.WARNING),
    INFO(Level.INFO),
    DEBUG(Level.FINE),
    TRACE(Level.FINER),
    ALL(Level.ALL); // OR FINEST?

    private final Level javaLogLevel;

    LogLevel(final Level javaLogLevel) {
        this.javaLogLevel = javaLogLevel;
    }

    public java.util.logging.Level toJavaLogLevel() {
        return javaLogLevel;
    }

    public static LogLevel nanoLogLevelOf(final Level level) {
        return Arrays.stream(LogLevel.values()).filter(simpleLogLevel -> simpleLogLevel.javaLogLevel == level).findFirst().orElse(OFF);

    }

    public static LogLevel nanoLogLevelOf(final String level) {
        if (hasText(level)) {
            // Nano log level
            for (final LogLevel logLevel : LogLevel.values()) {
                if (logLevel.toString().equalsIgnoreCase(level)) {
                    return logLevel;
                }
            }

            // Java log level
            for (final LogLevel logLevel : LogLevel.values()) {
                if (logLevel.javaLogLevel.toString().equalsIgnoreCase(level)) {
                    return logLevel;
                }
            }
        }
        return ALL;
    }
}
