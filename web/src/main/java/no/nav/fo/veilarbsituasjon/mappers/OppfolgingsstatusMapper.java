package no.nav.fo.veilarbsituasjon.mappers;

import no.nav.fo.veilarbsituasjon.rest.domain.Oppfolgingsstatus;
import no.nav.fo.veilarbsituasjon.rest.domain.Oppfolgingsenhet;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingsstatusResponse;

public class OppfolgingsstatusMapper {
    public static Oppfolgingsstatus tilOppfolgingsstatus(WSHentOppfoelgingsstatusResponse response, Oppfolgingsenhet oppfolgingsenhet) {
        return new Oppfolgingsstatus()
                .setOppfolgingsenhet(oppfolgingsenhet)
                .setRettighetsgruppe(response.getRettighetsgruppeKode())
                .setFormidlingsgruppe(response.getFormidlingsgruppeKode())
                .setServicegruppe(response.getServicegruppeKode());
    }
}
