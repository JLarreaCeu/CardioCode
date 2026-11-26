package commonInfo;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {

    private static final String FILE = "config.properties";
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 6767;
    private static Properties properties;

    private static synchronized Properties load() {
        if (properties == null) {
            properties = new Properties();
            try (FileInputStream in = new FileInputStream(FILE)) {
                properties.load(in);
            } catch (IOException e) {
                System.out.println("No " + FILE + " found, using default values");
            }
        }
        return properties;
    }

    public static String getServerHost() {
        String host = load().getProperty("server.host");
        if (host != null && !host.isBlank()) {
            return host.trim();
        }
        String found = ServerFinder.findServer();
        if (found != null) {
            return found;
        }
        return DEFAULT_HOST;
    }

    public static int getServerPort() {
        String port = load().getProperty("server.port");
        if (port == null || port.isBlank()) {
            return DEFAULT_PORT;
        }
        try {
            return Integer.parseInt(port.trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid server.port in " + FILE + ", using " + DEFAULT_PORT);
            return DEFAULT_PORT;
        }
    }
}
