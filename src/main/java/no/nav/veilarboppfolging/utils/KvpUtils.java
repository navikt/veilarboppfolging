package no.nav.veilarboppfolging.utils;

import lombok.SneakyThrows;
import no.nav.veilarboppfolging.domain.Kvp;
import no.nav.veilarboppfolging.service.AuthService;

import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Collections.singletonList;

public class KvpUtils {

    public static boolean sjekkTilgangGittKvp(AuthService authService, Kvp kvp, Supplier<Date> dateSupplier) {
        return kvp == null || sjekkTilgangGittKvp(authService, singletonList(kvp), dateSupplier);
    }

    @SneakyThrows
    public static boolean sjekkTilgangGittKvp(AuthService authService, List<Kvp> kvpList, Supplier<Date> dateSupplier) {
        for (Kvp kvp : kvpList) {
            if (between(kvp.getOpprettetDato(), kvp.getAvsluttetDato(), dateSupplier.get())) {
                return authService.harTilgangTilEnhet(kvp.getEnhet());
            }
        }
        return true;
    }

    private static boolean between(Date start, Date stop, Date date) {
        return !date.before(start) && (stop == null || !date.after(stop));
    }

}
