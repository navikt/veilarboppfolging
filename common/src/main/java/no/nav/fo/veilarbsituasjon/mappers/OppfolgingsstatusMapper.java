package no.nav.fo.veilarbsituasjon.mappers;

import no.nav.fo.veilarbsituasjon.domain.Oppfolgingsenhet;
import no.nav.fo.veilarbsituasjon.domain.Oppfolgingsstatus;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingsstatusResponse;

public class OppfolgingsstatusMapper {
    public static Oppfolgingsstatus tilOppfolgingsstatus(HentOppfoelgingsstatusResponse response, Oppfolgingsenhet oppfolgingsenhet) {
        return new Oppfolgingsstatus()
                .setOppfolgingsenhet(oppfolgingsenhet)
                .setRettighetsgruppe(response.getRettighetsgruppeKode())
                .setFormidlingsgruppe(response.getFormidlingsgruppeKode())
                .setServicegruppe(response.getServicegruppeKode());
    }
}
