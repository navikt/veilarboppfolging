package no.nav.veilarboppfolging.utils;

import java.util.Arrays;
import java.util.Optional;

public class EnumUtils {

    private EnumUtils(){}

    public static String getName(Enum<?> anEnum) {
        return anEnum != null ? anEnum.name() : null;
    }

    public static <T extends Enum> T valueOf(Class<T> enumClass, String name) {
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> e.name().equals(name))
                .findAny()
                .orElse(null);
    }

    public static <T extends Enum> Optional<T> valueOfOptional(Class<T> enumClass, String name) {
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> e.name().equals(name))
                .findAny();
    }

}

