package no.nav.fo.veilarboppfolging.utils;

import lombok.SneakyThrows;
import no.nav.apiapp.security.veilarbabac.VeilarbAbacPepClient;
import no.nav.fo.veilarboppfolging.domain.Kvp;

import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Collections.singletonList;

public class KvpUtils {

    public static boolean sjekkTilgangGittKvp(VeilarbAbacPepClient pepClient, Kvp kvp, Supplier<Date> dateSupplier) {
        return kvp == null || sjekkTilgangGittKvp(pepClient, singletonList(kvp), dateSupplier);

    }

    @SneakyThrows
    public static boolean sjekkTilgangGittKvp(VeilarbAbacPepClient pepClient, List<Kvp> kvpList, Supplier<Date> dateSupplier) {
        for (Kvp kvp : kvpList) {
            if (between(kvp.getOpprettetDato(), kvp.getAvsluttetDato(), dateSupplier.get())) {
                return pepClient.harTilgangTilEnhet(kvp.getEnhet());
            }
        }
        return true;
    }

    private static boolean between(Date start, Date stop, Date date) {
        return !date.before(start) && (stop == null || !date.after(stop));
    }

}
