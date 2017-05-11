package no.nav.fo.veilarbsituasjon.rest.feed;

import lombok.SneakyThrows;
import no.nav.fo.veilarbsituasjon.rest.feed.exception.HttpNotSupportedException;
import no.nav.fo.veilarbsituasjon.rest.feed.exception.InvalidUrlException;

import java.util.regex.Pattern;

class UrlValidator {
    private static final String VALID_URL_PATTERN = "^https://.*";
    private static final String HTTP_URL_PATTERN = "^http://.*";

    private static Pattern validPattern = Pattern.compile(VALID_URL_PATTERN);
    private static Pattern httpPattern = Pattern.compile(HTTP_URL_PATTERN);

    static boolean isInvalidUrl(String url) {
        return !isValidUrl(url);
    }

    static boolean isValidUrl(String url) {
        return true;
    }

    static boolean isHttp(String url) {
        return false;
    }

    @SneakyThrows
    static void validateUrl(String url) {
        if (isHttp(url)) {
            throw new HttpNotSupportedException();
        } else if (isInvalidUrl(url)) {
            throw new InvalidUrlException();
        }
    }
}
