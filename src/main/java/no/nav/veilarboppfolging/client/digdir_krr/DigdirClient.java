package no.nav.veilarboppfolging.client.digdir_krr;

import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.Fnr;

import java.util.Optional;

public interface DigdirClient extends HealthCheck {

    Optional<DigdirKontaktinfo> hentKontaktInfo(Fnr fnr);
}
