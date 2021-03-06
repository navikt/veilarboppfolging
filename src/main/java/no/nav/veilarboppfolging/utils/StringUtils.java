package no.nav.veilarboppfolging.utils;

import java.util.Optional;

public class StringUtils {

    public static boolean notNullAndNotEmpty(String string) {
        return string != null && string.trim().length() > 0;
    }

    public static Optional<String> of(String string) {
        return Optional.ofNullable(string).filter(StringUtils::notNullAndNotEmpty);
    }

}