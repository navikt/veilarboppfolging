package no.nav.veilarboppfolging.client.behandle_arbeidssoker;

import no.nav.common.health.HealthCheck;
import no.nav.veilarboppfolging.domain.Fnr;
import no.nav.veilarboppfolging.domain.Innsatsgruppe;

public interface BehandleArbeidssokerClient extends HealthCheck {

    void opprettBrukerIArena(String fnr, Innsatsgruppe innsatsgruppe);

    void reaktiverBrukerIArena(Fnr fnr);

}
