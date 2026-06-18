package no.nav.veilarboppfolging.utils;

import java.time.ZonedDateTime;


public class DateUtils {
    public static boolean between(ZonedDateTime start, ZonedDateTime stop, ZonedDateTime date) {
        return !date.isBefore(start) && (stop == null || !date.isAfter(stop));
    }
}
