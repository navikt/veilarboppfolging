package no.nav.veilarboppfolging.client.digdir_krr;

import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.Fnr;

import java.util.Optional;

public interface DigdirClient extends HealthCheck {

    /**
     * Bruk hentKontaktInfoV2 istdet, den bruker v2 fra KRR
     * **/
    @Deprecated
    Optional<KRRData> hentKontaktInfo(Fnr fnr);
    Optional<KRRData> hentKontaktInfoV2(Fnr fnr);
}
