package no.nav.veilarboppfolging.client.veilarbportefolje;

import no.nav.common.health.HealthCheck;

public interface VeilarbportefoljeClient extends HealthCheck {

    OppfolgingEnhetPageDTO hentEnhetPage(int pageNumber, int pageSize);

}
