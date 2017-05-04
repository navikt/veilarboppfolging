package no.nav.fo.veilarbsituasjon.utils;

import java.net.MalformedURLException;
import java.util.regex.Pattern;

public class UrlValidator {
    private static final String URL_PATTERN = "^https://.*";

    private static Pattern pattern = Pattern.compile(URL_PATTERN);

    public static boolean isInvalidUrl(String url) {
        return !isValidUrl(url);
    }

    public static boolean isValidUrl(String url) {
        return pattern.matcher(url).matches();
    }

    public static void validateUrl(String url) throws MalformedURLException {
        if (isInvalidUrl(url)) {
            throw new MalformedURLException();
        }
    }
}
