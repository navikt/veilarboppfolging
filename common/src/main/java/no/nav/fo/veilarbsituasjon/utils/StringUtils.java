package no.nav.fo.veilarbsituasjon.utils;

import java.util.Optional;

public class StringUtils {

    public static boolean notNullAndNotEmpty(String string) {
        return string != null && string.trim().length() > 0;
    }

    public static Optional<String> of(String string) {
        return Optional.ofNullable(string).filter(StringUtils::notNullAndNotEmpty);
    }

    public static String emptyIfNull(String string) {
        return of(string).orElse("");
    }
}