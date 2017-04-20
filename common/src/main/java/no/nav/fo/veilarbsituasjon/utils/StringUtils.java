package no.nav.fo.veilarbsituasjon.utils;

import java.util.Optional;

public class StringUtils {

    public static boolean notNullOrEmpty(String string) {
        return string != null && string.trim().length() > 0;
    }

    public static Optional<String> of(String string) {
        return Optional.ofNullable(string).filter(StringUtils::notNullOrEmpty);
    }

}