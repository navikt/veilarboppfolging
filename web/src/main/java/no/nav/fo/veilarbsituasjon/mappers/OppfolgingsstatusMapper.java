package no.nav.fo.veilarbsituasjon.mappers;

import no.nav.fo.veilarbsituasjon.rest.domain.Oppfolgingsstatus;
import no.nav.fo.veilarbsituasjon.rest.domain.Organisasjonsenhet;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.WSHentOppfoelgingsstatusResponse;

public class OppfolgingsstatusMapper {
    public static Oppfolgingsstatus tilOppfolgingsstatus(WSHentOppfoelgingsstatusResponse response, Organisasjonsenhet organisasjonsenhet) {
        return new Oppfolgingsstatus()
                .setOrganisasjonsenhet(organisasjonsenhet)
                .setRettighetsgruppe(response.getRettighetsgruppeKode())
                .setFormidlingsgruppe(response.getFormidlingsgruppeKode())
                .setServicegruppe(response.getServicegruppeKode());
    }
}
