package no.nav.veilarboppfolging.utils;

import java.util.List;
import java.util.Optional;

public class ListUtils {

    public static <T> T firstOrNull(List<T> list) {
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    public static <T> Optional<T> maybeFirst(List<T> list) {
        return list == null || list.isEmpty() ? Optional.empty() : Optional.ofNullable(list.get(0));
    }

}
