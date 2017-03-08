package no.nav.fo.veilarbsituasjon.mappers;

import no.nav.fo.veilarbsituasjon.domain.Oppfolgingsstatus;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingsstatusResponse;

public class OppfolgingsstatusMapper {
    public static Oppfolgingsstatus tilOppfolgingsstatus(WSHentOppfoelgingsstatusResponse response) {
        return new Oppfolgingsstatus()
                .setOppfolginsenhet(response.getNavOppfoelgingsenhet())
                .setRettighetsgruppe(response.getRettighetsgruppeKode())
                .setFormidlingsgruppe(response.getFormidlingsgruppeKode())
                .setServicegruppe(response.getServicegruppeKode());
    }
}
