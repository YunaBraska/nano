package berlin.yuna.nano.core;

import berlin.yuna.nano.helper.logger.logic.NanoLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private static final Properties properties = new Properties();
    protected static final NanoLogger logger = new NanoLogger(Config.class);

    private Config() {
    }

    static {
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            properties.load(input);
        } catch (IOException ex) {
            logger.error(ex, () -> "Failed to load config.properties");
        }
    }

    public static String getMetricsBaseUrl() {
        return properties.getProperty("metrics.base.url");
    }

    public static String getPrometheusBaseUrl() {
        return properties.getProperty("prometheus.metrics.url");
    }

    public static String getInfluxBaseUrl() {
        return properties.getProperty("influx.metrics.url");
    }

    public static String getWavefrontBaseUrl() {
        return properties.getProperty("wavefront.metrics.url");
    }

    public static String getDynamoBaseUrl() {
        return properties.getProperty("dynamo.metrics.url");
    }
}
