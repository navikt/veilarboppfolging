package no.nav.veilarboppfolging.feed.cjm.util;

import lombok.SneakyThrows;

import java.util.function.Supplier;

public class MetricsUtils {

    @SneakyThrows
    public static <S> S timed(String name, Supplier<S> supplier) {
        return null;
//        return (S) MetodeTimer.timeMetode(supplier::get, name);
    }

    public static void metricEvent(String eventName, String feedName) {
//        createEvent("feed." + eventName)
//                .addTagToReport("feedname", feedName)
//                .report();
    }

}
