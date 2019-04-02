package no.nav.fo.veilarboppfolging.mappers;

import no.nav.fo.veilarboppfolging.domain.ArenaOppfolging;

import static no.nav.fo.veilarboppfolging.utils.DateUtils.xmlGregorianCalendarToLocalDate;

public class ArenaOppfolgingMapper {

    public static ArenaOppfolging mapTilArenaOppfolgingsstatusV2(no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.meldinger.HentOppfoelgingsstatusResponse response) {
        return new ArenaOppfolging()
                .setOppfolgingsenhet(response.getNavOppfoelgingsenhet())
                .setRettighetsgruppe(response.getRettighetsgruppeKode().getValue())
                .setFormidlingsgruppe(response.getFormidlingsgruppeKode().getValue())
                .setServicegruppe(response.getServicegruppeKode().getValue())
                .setInaktiveringsdato(xmlGregorianCalendarToLocalDate(response.getInaktiveringsdato()))
                .setHarMottaOppgaveIArena(response.isHarOppgaveMottaSelvregPerson())
                .setKanEnkeltReaktiveres(response.isKanEnkeltReaktiveres());
    }
}
