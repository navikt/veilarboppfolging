package no.nav.fo.veilarboppfolging.utils;

import lombok.SneakyThrows;
import no.nav.sbl.util.EnvironmentUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

public class NaiseratorUtils {

    private static String DEFAULT_SECRETS_PATH = "/var/run/secrets/nais.io";
    private static String DEFAULT_CREDENTIALS_USERNAME_FILE = "username";
    private static String DEFAULT_CREDENTIALS_PASSWORD_FILE = "password";

    public static String CONFIG_MAPS_PATH = "CONFIG_MAPS_PATH";
    private static String DEFAULT_CONFIG_MAPS_PATH = "/var/run/configmaps";

    public static class Credentials {
        public final String username;
        public final String password;

        private Credentials(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    public static Credentials getCredentials(String credentialsPath,
                                             String usernameFileName,
                                             String passwordFileName) {
        Path path = Paths.get(credentialsPath);
        String username = getFileContent(path.resolve(usernameFileName));
        String password = getFileContent(path.resolve(passwordFileName));

        return new Credentials(username, password);
    }

    public static Credentials getCredentials(String path) {
        return getCredentials(path, DEFAULT_CREDENTIALS_USERNAME_FILE, DEFAULT_CREDENTIALS_PASSWORD_FILE);
    }

    public static String getFileContent(String path) {
        return getFileContent(Paths.get(path));
    }


    private static String getFileContent(Path path) {
        return Optional.of(path)
                .filter(Files::isRegularFile)
                .map(NaiseratorUtils::readAllLines)
                .flatMap(lines ->
                        lines.stream().reduce((a, b) -> a + "\n" + b))
                .orElseThrow(() -> new IllegalStateException(format("Fant ikke fil %s", path.toString())));
    }

    @SneakyThrows
    private static List<String> readAllLines(Path path) {
        return Files.readAllLines(path);
    }

    public static String getDefaultSecretPath(String secret) {
        return Paths.get(DEFAULT_SECRETS_PATH, secret).toString();
    }

    public static void addConfigMapToEnv(String configMap) {
        addMapToEnv(readConfigMap(configMap));
    }

    public static void addConfigMapToEnv(String configMap, String... keys) {
        addMapToEnv(readConfigMap(configMap, keys));
    }

    private static void addMapToEnv(Map<String, String> map) {
        map.entrySet().forEach(entrySet -> System.setProperty(entrySet.getKey(), entrySet.getValue()));
    }

    @SneakyThrows
    public static Map<String, String> readConfigMap(String configMap) {

        String configMapsPath = EnvironmentUtils.getOptionalProperty(CONFIG_MAPS_PATH).orElse(DEFAULT_CONFIG_MAPS_PATH);
        Path path = Paths.get(configMapsPath, configMap);
        Stream<Path> files = Files.walk(path, 1).filter(Files::isRegularFile);

        return files.collect(Collectors.toMap(file -> file.getFileName().toString(), NaiseratorUtils::getFileContent));
    }

    public static Map<String, String> readConfigMap(String configMap, String... keys) {
        List<String> keyList = Arrays.asList(keys);
        return readConfigMap(configMap).entrySet().stream()
                .filter(entrySet -> keyList.contains(entrySet.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}

