package no.nav.fo.veilarboppfolging.services;

import no.nav.fo.veilarboppfolging.db.OppfolgingsenhetHistorikkRepository;
import no.nav.fo.veilarboppfolging.domain.OppfolgingsenhetEndringData;
import no.nav.fo.veilarboppfolging.mappers.VeilarbArenaOppfolging;

import javax.inject.Inject;
import java.util.List;

public class OppfolgingsenhetEndringService {
    @Inject
    private OppfolgingsenhetHistorikkRepository oppfolgingsenhetHistorikkRepository;

    public void behandleBrukerEndring(VeilarbArenaOppfolging veilarbArenaOppfolging) {
        List<OppfolgingsenhetEndringData> oppfolgingsenhetEndringData = oppfolgingsenhetHistorikkRepository.hentOppfolgingsenhetEndringerForAktorId(veilarbArenaOppfolging.getAktoerid());

        //TODO: legge til alle endringer på enhet på en eller annen måte ...
    }
}
