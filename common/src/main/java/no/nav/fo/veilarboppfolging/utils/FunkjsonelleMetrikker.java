package no.nav.fo.veilarboppfolging.utils;

import no.nav.fo.veilarboppfolging.domain.Tilordning;
import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;

import java.util.Date;
import java.util.Optional;

public class FunkjsonelleMetrikker {
    public static Tilordning lestAvVeileder(Tilordning tilordning) {
        Event event = MetricsFactory.createEvent("tilordnet.veileder.lest");
        Optional.of(tilordning)
                .map(Tilordning::getSistTilordnet)
                .map(Date::getTime)
                .map(FunkjsonelleMetrikker::msSiden)
                .ifPresent(ms -> event.addFieldToReport("ms", ms));
        event.report();
        return tilordning;
    }

    private static long msSiden(long time){
        return new Date().getTime() - time;
    }

    private FunkjsonelleMetrikker() {
    }
}
