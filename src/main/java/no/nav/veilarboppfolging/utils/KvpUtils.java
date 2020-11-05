package no.nav.veilarboppfolging.utils;

import lombok.SneakyThrows;
import no.nav.veilarboppfolging.domain.Kvp;
import no.nav.veilarboppfolging.service.AuthService;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Collections.singletonList;

public class KvpUtils {

    public static boolean sjekkTilgangGittKvp(AuthService authService, Kvp kvp, Supplier<ZonedDateTime> dateSupplier) {
        return kvp == null || sjekkTilgangGittKvp(authService, singletonList(kvp), dateSupplier);
    }

    @SneakyThrows
    public static boolean sjekkTilgangGittKvp(AuthService authService, List<Kvp> kvpList, Supplier<ZonedDateTime> dateSupplier) {
        for (Kvp kvp : kvpList) {
            if (between(kvp.getOpprettetDato(), kvp.getAvsluttetDato(), dateSupplier.get())) {
                return authService.harTilgangTilEnhetMedSperre(kvp.getEnhet());
            }
        }
        return true;
    }

    private static boolean between(ZonedDateTime start, ZonedDateTime stop, ZonedDateTime date) {
        return !date.isBefore(start) && (stop == null || !date.isAfter(stop));
    }

}
