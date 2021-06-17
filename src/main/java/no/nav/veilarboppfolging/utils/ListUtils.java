package no.nav.veilarboppfolging.utils;

import java.util.List;

public class ListUtils {

    public static <T> T firstOrNull(List<T> list) {
        return list == null || list.isEmpty() ? null : list.get(0);
    }

}
