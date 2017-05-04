package no.nav.fo.veilarbsituasjon.utils;

import no.nav.fo.veilarbsituasjon.exception.HttpNotSupportedException;

import java.net.MalformedURLException;
import java.util.regex.Pattern;

public class UrlValidator {
    private static final String VALID_URL_PATTERN = "^https://.*";
    private static final String HTTP_URL_PATTERN = "^http://.*";

    private static Pattern validPattern = Pattern.compile(VALID_URL_PATTERN);
    private static Pattern httpPattern = Pattern.compile(HTTP_URL_PATTERN);

    static boolean isInvalidUrl(String url) {
        return !isValidUrl(url);
    }

    static boolean isValidUrl(String url) {
        return validPattern.matcher(url).matches();
    }

    static boolean isHttp(String url) {
        return httpPattern.matcher(url).matches();
    }

    public static void validateUrl(String url) throws MalformedURLException, HttpNotSupportedException {
        if (isHttp(url)) {
            throw new HttpNotSupportedException();
        } else if (isInvalidUrl(url)) {
            throw new MalformedURLException();
        }
    }
}
