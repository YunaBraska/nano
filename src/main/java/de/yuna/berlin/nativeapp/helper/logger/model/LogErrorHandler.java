package de.yuna.berlin.nativeapp.helper.logger.model;

import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

@SuppressWarnings({"java:S106", "unused"})
public class LogErrorHandler extends StreamHandler {

    public LogErrorHandler(final Formatter formatter) {
        super(System.err, formatter);
        setLevel(Level.ALL);
    }

    @Override
    public void publish(final LogRecord logRecord) {
        if (logRecord.getLevel().intValue() >= Level.WARNING.intValue()) {
            super.publish(logRecord);
            flush();
        }
    }
}

