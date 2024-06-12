package berlin.yuna.nano.helper;

import berlin.yuna.nano.core.NanoBase;
import berlin.yuna.nano.core.NanoServices;
import berlin.yuna.nano.core.NanoThreads;
import berlin.yuna.nano.core.model.Context;
import berlin.yuna.nano.core.model.NanoThread;
import berlin.yuna.nano.core.model.Scheduler;
import berlin.yuna.nano.core.model.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.zip.DeflaterInputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static berlin.yuna.nano.core.NanoBase.standardiseKey;
import static berlin.yuna.nano.core.model.Context.CONFIG_PROFILES;
import static java.util.Arrays.stream;

@SuppressWarnings({"UnusedReturnValue", "java:S6548"})
public class NanoUtils {

    public static final String LINE_SEPARATOR = System.lineSeparator();
    protected static Random random;
    public static final String[][] NANO_NAMES = {
        {"NanoNinja", "NanoNoodle", "GraalGuru", "JavaJester", "MicroMaverick", "ByteBender", "NanoNaut", "GraalGoblin", "JavaJuggernaut", "CodeComedian", "NanoNomad", "GraalGazelle", "JavaJinx", "MicroMagician", "ByteBandit", "NanoNimbus", "GraalGambler", "JavaJester", "MicroMaestro", "ByteBarracuda", "NanoNebula"},
        {"Swift Swiper", "Master", "Joker", "Rebel", "Twister", "Navigator", "Mischievous", "Unstoppable", "Laughs", "Wanderer", "Graceful", "Bringer", "Wizard", "Stealer", "Cloud Surfer", "Betting", "Prankster", "Conductor", "Feisty Fish", "Galactic Guardian"},
        {"of Requests", "of Native Magic", "in the Microservice Deck", "in the Server Space", "of Bytes", "of the Nano Cosmos", "Microservice Minion", "Force of the JVM", "in Lambda Expressions", "in the Backend Wilderness", "GraalVM Gazelle", "Bringer of Backend Blessings", "of the Microservice Realm", "of Server Secrets", "of the Nanoverse", "on Backend Brilliance", "in the Programming Playground", "of the Microservice Orchestra", "in the Server Sea", "of Microservices", "of Nano Power", "in Nano Land", "near Nano Destiny"}};

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

    public static String[] split(final String input, final String delimiter) {
        if (!input.contains(delimiter)) {
            return new String[]{input};
        }
        final List<String> result = new ArrayList<>();
        int start = 0;
        int index;
        while ((index = input.indexOf(delimiter, start)) != -1) {
            result.add(input.substring(start, index));
            start = index + delimiter.length();
        }
        result.add(input.substring(start));
        return result.toArray(new String[0]);
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
            NanoThread.class.getName(),
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

    // ########## NANO CONFIGS ##########
    public static Context readConfigFiles(final Context context, final String profile) {
        final Context result = context != null ? context : Context.createRootContext();
        final List<String> scannedProfiles = result.getList(ArrayList::new, String.class, "_scanned_profiles");
        if (scannedProfiles.contains(profile))
            return result;
        if (!"".equals(profile))
            scannedProfiles.add(profile);
        result.put("_scanned_profiles", scannedProfiles);

        for (final String directory : new String[]{
            "",
            ".",
            "config/",
            ".config/",
            "resources/",
            ".resources/",
            "resources/config/",
            ".resources/config/"
        }) {
            readConfigFile(result, directory + "application" + (profile.isEmpty() ? profile : "-" + profile) + ".properties");
        }
        return readProfiles(result);
    }

    public static Context readProfiles(final Context result) {
        for (final String pConfig : new String[]{
            CONFIG_PROFILES,
            "app_profile",
            "spring_profiles_active",
            "spring_profile_active",
            "profiles_active",
            "micronaut_profiles",
            "micronaut_environments"
        }) {
            result.getOpt(String.class, pConfig).ifPresent(profiles -> stream(split(profiles, ",")).map(String::trim).forEach(name -> readConfigFiles(result, name)));
        }
        return result;
    }

    public static Context readConfigFile(final Context context, final String path) {
        try (final InputStream input = path.startsWith(".") ? new FileInputStream(path.substring(1)) : NanoUtils.class.getClassLoader().getResourceAsStream(path)) {
            if (input != null) {
                final Properties properties = new Properties();
                properties.load(input);
                properties.forEach((key, value) -> addConfig(context, key, value));
            }
        } catch (final Exception ignored) {
            // ignored
        }
        return context;
    }

    public static Context addConfig(final Context context, final Object key, final Object value) {
        if (value == null || "null".equals(value) || "".equals(value)) {
            context.remove(standardiseKey(key));
        } else if (value instanceof final String valueStr && hasText(valueStr)) {
            context.put(standardiseKey(key), valueStr.trim());
        } else {
            context.put(standardiseKey(key), value);
        }
        return context;
    }

    public static Context resolvePlaceHolders(final Context context) {
        context.forEach((key, value) -> {
            if (value instanceof final String valueStr && valueStr.startsWith("${") && valueStr.endsWith("}")) {
                final String[] placeholder = split(valueStr.substring(2, valueStr.length() - 1), ":");
                addConfig(context, key, context.getOpt(Object.class, standardiseKey(placeholder[0])).orElseGet(() -> placeholder.length > 1 ? placeholder[1].trim() : null));
            }
        });
        return context;
    }

    public static byte[] encodeGzip(final byte[] data) {
        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
            gzipOutputStream.write(data);
            gzipOutputStream.finish();
            return outputStream.toByteArray();
        } catch (final IOException ignored) {
            return data;
        }
    }

    public static byte[] decodeGzip(final byte[] data) {
        try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
             final GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream)) {
            return gzipInputStream.readAllBytes();
        } catch (final Exception ignored) {
            return data;
        }
    }

    public static byte[] decoderDeflate(final byte[] data) {
        try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
             final DeflaterInputStream deflaterInputStream = new DeflaterInputStream(inputStream)) {
            return deflaterInputStream.readAllBytes();
        } catch (final Exception ignored) {
            return data;
        }
    }

    public static byte[] encodeDeflate(final byte[] data) {
        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             final DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(outputStream)) {
            deflaterOutputStream.write(data);
            deflaterOutputStream.finish();
            return outputStream.toByteArray();
        } catch (final IOException ignored) {
            return data;
        }
    }

    public static String generateNanoName(final String format) {
        if (random == null) {
            random = new Random();
        }
        return String.format(format,
            NANO_NAMES[0][random.nextInt(NANO_NAMES[0].length)],
            random.nextInt(0, 99) + "." + random.nextInt(0, 9),
            NANO_NAMES[1][random.nextInt(NANO_NAMES[1].length)],
            NANO_NAMES[2][random.nextInt(NANO_NAMES[2].length)]
        );
    }

    private NanoUtils() {
        // static util class
    }
}
