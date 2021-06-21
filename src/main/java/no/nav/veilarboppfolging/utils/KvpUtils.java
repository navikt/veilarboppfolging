package no.nav.veilarboppfolging.utils;

import no.nav.veilarboppfolging.repository.entity.KvpEntity;
import no.nav.veilarboppfolging.service.AuthService;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Collections.singletonList;

public class KvpUtils {

    public static boolean sjekkTilgangGittKvp(AuthService authService, KvpEntity kvp, Supplier<ZonedDateTime> dateSupplier) {
        return kvp == null || sjekkTilgangGittKvp(authService, singletonList(kvp), dateSupplier);
    }

    public static boolean sjekkTilgangGittKvp(AuthService authService, List<KvpEntity> kvpList, Supplier<ZonedDateTime> dateSupplier) {
        for (KvpEntity kvp : kvpList) {
            if (DateUtils.between(kvp.getOpprettetDato(), kvp.getAvsluttetDato(), dateSupplier.get())) {
                return authService.harTilgangTilEnhetMedSperre(kvp.getEnhet());
            }
        }
        return true;
    }

}
