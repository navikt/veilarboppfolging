package no.nav.fo.veilarboppfolging.mappers;

import no.nav.fo.veilarboppfolging.domain.ArenaOppfolging;
import no.nav.fo.veilarboppfolging.domain.Oppfolgingsenhet;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingsstatusResponse;

import static no.nav.fo.veilarboppfolging.utils.DateUtils.xmlGregorianCalendarToLocalDate;

public class ArenaOppfolgingMapper {
    public static ArenaOppfolging mapTilArenaOppfolging(HentOppfoelgingsstatusResponse response, Oppfolgingsenhet oppfolgingsenhet) {
        return new ArenaOppfolging()
                .setOppfolgingsenhet(oppfolgingsenhet)
                .setRettighetsgruppe(response.getRettighetsgruppeKode())
                .setFormidlingsgruppe(response.getFormidlingsgruppeKode())
                .setServicegruppe(response.getServicegruppeKode())
                .setInaktiveringsdato(xmlGregorianCalendarToLocalDate(response.getInaktiveringsdato()));
    }

    public static ArenaOppfolging mapTilArenaOppfolgingsstatus(no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v1.meldinger.HentOppfoelgingsstatusResponse response,
                                                               Oppfolgingsenhet oppfolgingsenhet) {
        return new ArenaOppfolging()
                .setOppfolgingsenhet(oppfolgingsenhet)
                .setRettighetsgruppe(response.getRettighetsgruppeKode().toString())
                .setFormidlingsgruppe(response.getFormidlingsgruppeKode().getKodeverksRef())
                .setServicegruppe(response.getServicegruppeKode().getKodeverksRef())
                .setInaktiveringsdato(xmlGregorianCalendarToLocalDate(response.getInaktiveringsdato()));
    }
}
