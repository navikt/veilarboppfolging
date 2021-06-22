package no.nav.veilarboppfolging.utils;

import no.nav.veilarboppfolging.repository.entity.KvpPeriodeEntity;
import no.nav.veilarboppfolging.service.AuthService;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Collections.singletonList;

public class KvpUtils {

    public static boolean sjekkTilgangGittKvp(AuthService authService, KvpPeriodeEntity kvp, Supplier<ZonedDateTime> dateSupplier) {
        return kvp == null || sjekkTilgangGittKvp(authService, singletonList(kvp), dateSupplier);
    }

    public static boolean sjekkTilgangGittKvp(AuthService authService, List<KvpPeriodeEntity> kvpList, Supplier<ZonedDateTime> dateSupplier) {
        for (KvpPeriodeEntity kvp : kvpList) {
            if (DateUtils.between(kvp.getOpprettetDato(), kvp.getAvsluttetDato(), dateSupplier.get())) {
                return authService.harTilgangTilEnhetMedSperre(kvp.getEnhet());
            }
        }
        return true;
    }

}
