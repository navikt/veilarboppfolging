package no.nav.veilarboppfolging.client.behandle_arbeidssoker;

import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.request.Innsatsgruppe;

public interface BehandleArbeidssokerClient extends HealthCheck {
    void reaktiverBrukerIArena(Fnr fnr);
}
