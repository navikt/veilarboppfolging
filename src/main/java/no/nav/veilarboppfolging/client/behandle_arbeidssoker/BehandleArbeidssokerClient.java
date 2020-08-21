package no.nav.veilarboppfolging.client.behandle_arbeidssoker;

import no.nav.common.health.HealthCheck;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeResponse;
import no.nav.veilarboppfolging.domain.Fnr;
import no.nav.veilarboppfolging.domain.Innsatsgruppe;

import javax.xml.datatype.XMLGregorianCalendar;

public interface BehandleArbeidssokerClient extends HealthCheck {

    void opprettBrukerIArena(String fnr, Innsatsgruppe innsatsgruppe);

    void reaktiverBrukerIArena(Fnr fnr);

}
