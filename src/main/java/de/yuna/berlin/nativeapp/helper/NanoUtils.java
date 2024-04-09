package de.yuna.berlin.nativeapp.helper;

import berlin.yuna.typemap.config.TypeConversionRegister;
import berlin.yuna.typemap.logic.JsonDecoder;
import berlin.yuna.typemap.model.LinkedTypeMap;
import berlin.yuna.typemap.model.TypeContainer;
import berlin.yuna.typemap.model.TypeList;
import de.yuna.berlin.nativeapp.core.NanoBase;
import de.yuna.berlin.nativeapp.core.NanoServices;
import de.yuna.berlin.nativeapp.core.NanoThreads;
import de.yuna.berlin.nativeapp.core.model.Scheduler;
import de.yuna.berlin.nativeapp.core.model.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

@SuppressWarnings({"UnusedReturnValue", "java:S6548"})
public class NanoUtils {

    public static final String LINE_SEPARATOR = System.lineSeparator();

    public static boolean hasText(final String str) {
        return (str != null && !str.isEmpty() && containsText(str));
    }

    public static String formatDuration(final long milliseconds) {
        final long years = TimeUnit.MILLISECONDS.toDays(milliseconds) / 365;
        long remainder = milliseconds - TimeUnit.DAYS.toMillis(years * 365);
        final long months = TimeUnit.MILLISECONDS.toDays(remainder) / 30;
        remainder -= TimeUnit.DAYS.toMillis(months * 30);
        final long days = TimeUnit.MILLISECONDS.toDays(remainder);
        remainder -= TimeUnit.DAYS.toMillis(days);
        final long hours = TimeUnit.MILLISECONDS.toHours(remainder);
        remainder -= TimeUnit.HOURS.toMillis(hours);
        final long minutes = TimeUnit.MILLISECONDS.toMinutes(remainder);
        remainder -= TimeUnit.MINUTES.toMillis(minutes);
        final long seconds = TimeUnit.MILLISECONDS.toSeconds(remainder);
        remainder -= TimeUnit.SECONDS.toMillis(seconds);

        final StringBuilder sb = new StringBuilder();
        if (years > 0) sb.append(years).append("y ");
        if (months > 0) sb.append(months).append("mo ");
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0) sb.append(seconds).append("s ");
        if (remainder > 0) sb.append(remainder).append("ms");

        return sb.toString().trim();
    }


    /**
     * Waits for a condition to become true, with actions on success or timeout.
     *
     * @param condition The condition to wait for, returning true when met.
     * @return true if the condition was met within the timeout, false otherwise.
     */
    public static boolean waitForCondition(final BooleanSupplier condition) {
        return waitForCondition(condition, 2000);
    }

    /**
     * Waits for a condition to become true, with actions on success or timeout.
     *
     * @param condition The condition to wait for, returning true when met.
     * @param timeout   stops waiting after period of time to unblock the test.
     * @return true if the condition was met within the timeout, false otherwise.
     */
    public static boolean waitForCondition(final BooleanSupplier condition, final long timeout) {
        final long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeout) {
            if (condition.getAsBoolean()) {
                return true;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(64);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return false;
    }

    private static boolean containsText(final CharSequence str) {
        final int strLen = str.length();
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    public static String callerInfoStr(final Class<?> source) {
        final StackTraceElement element = callerInfo(source);
        return element == null ? "Unknown" : String.format("%s:%d_at_%s", element.getClassName(), element.getLineNumber(), element.getMethodName());
    }

    public static StackTraceElement callerInfo(final Class<?> source) {
        final List<String> sourceNames = List.of(
                source.getName(),
                Service.class.getName(),
                NanoBase.class.getName(),
                NanoUtils.class.getName(),
                NanoThreads.class.getName(),
                NanoServices.class.getName()
        );
        final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        for (final StackTraceElement element : stackTrace) {
            if (!sourceNames.contains(element.getClassName()) && !element.getClassName().startsWith("java.lang.Thread")) {
                return element;
            }
        }
        return stackTrace.length > 2 ? stackTrace[2] : null;
    }

    public static String getThreadName(final ExecutorService executorService) {
        if (executorService instanceof final Scheduler scheduler) {
            return scheduler.id();
        } else if (executorService instanceof ScheduledExecutorService) {
            return "Scheduler";
        }
        return executorService.getClass().getSimpleName();
    }

    private NanoUtils() {
        // static util class
    }
}
