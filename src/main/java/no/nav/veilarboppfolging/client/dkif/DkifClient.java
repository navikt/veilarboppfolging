package no.nav.veilarboppfolging.client.dkif;

import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.Fnr;

public interface DkifClient extends HealthCheck {

    DkifKontaktinfo hentKontaktInfo(Fnr fnr);

}
