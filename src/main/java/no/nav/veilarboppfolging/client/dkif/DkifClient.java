package no.nav.veilarboppfolging.client.dkif;

import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.Fnr;

import java.util.Optional;

public interface DkifClient extends HealthCheck {

    Optional<DkifKontaktinfo> hentKontaktInfo(Fnr fnr);

}
