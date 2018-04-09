package no.nav.fo.veilarboppfolging.mappers;

import no.nav.fo.veilarboppfolging.domain.ArenaOppfolging;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingsstatusResponse;

import static no.nav.fo.veilarboppfolging.utils.DateUtils.xmlGregorianCalendarToLocalDate;

public class ArenaOppfolgingMapper {
    public static ArenaOppfolging mapTilArenaOppfolging(HentOppfoelgingsstatusResponse response) {
        return new ArenaOppfolging()
                .setOppfolgingsenhet(response.getNavOppfoelgingsenhet())
                .setRettighetsgruppe(response.getRettighetsgruppeKode())
                .setFormidlingsgruppe(response.getFormidlingsgruppeKode())
                .setServicegruppe(response.getServicegruppeKode())
                .setInaktiveringsdato(xmlGregorianCalendarToLocalDate(response.getInaktiveringsdato()));
    }

    public static ArenaOppfolging mapTilArenaOppfolgingsstatus(no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.meldinger.HentOppfoelgingsstatusResponse response) {
        return new ArenaOppfolging()
                .setOppfolgingsenhet(response.getNavOppfoelgingsenhet())
                .setRettighetsgruppe(response.getRettighetsgruppeKode().getValue())
                .setFormidlingsgruppe(response.getFormidlingsgruppeKode().getValue())
                .setServicegruppe(response.getServicegruppeKode().getValue())
                .setInaktiveringsdato(xmlGregorianCalendarToLocalDate(response.getInaktiveringsdato()))
                .setHarMottaOppgaveIArena(response.getHarOppgaveMottaSelvregPerson());
    }
}
