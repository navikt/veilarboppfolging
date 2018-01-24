package no.nav.fo.veilarboppfolging.utils;

import no.nav.fo.veilarboppfolging.domain.Kvp;
import no.nav.fo.veilarboppfolging.services.EnhetPepClient;

import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Collections.singletonList;

public class KvpUtils {

    public static boolean sjekkTilgangGittKvp(EnhetPepClient enhetPepClient, Kvp kvp, Supplier<Date> dateSupplier) {
        return kvp == null || sjekkTilgangGittKvp(enhetPepClient, singletonList(kvp), dateSupplier);

    }

    public static boolean sjekkTilgangGittKvp(EnhetPepClient enhetPepClient, List<Kvp> kvpList, Supplier<Date> dateSupplier) {
        for (Kvp kvp : kvpList) {
            if (between(kvp.getOpprettetDato(), kvp.getAvsluttetDato(), dateSupplier.get())) {
                return enhetPepClient.harTilgang(kvp.getEnhet());
            }
        }
        return true;
    }

    private static boolean between(Date start, Date stop, Date date) {
        return !date.before(start) && (stop == null || !date.after(stop));
    }

}
