package no.nav.veilarboppfolging.client.behandle_arbeidssoker;

import no.nav.common.health.HealthCheck;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.domain.Innsatsgruppe;

public interface BehandleArbeidssokerClient extends HealthCheck {

    void opprettBrukerIArena(Fnr fnr, Innsatsgruppe innsatsgruppe);

    void reaktiverBrukerIArena(Fnr fnr);

}
