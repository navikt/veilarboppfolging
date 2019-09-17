package no.nav.fo.veilarboppfolging.services;

import no.nav.fo.veilarboppfolging.db.OppfolgingsenhetHistorikkRepository;
import no.nav.fo.veilarboppfolging.domain.OppfolgingsenhetEndringData;
import no.nav.fo.veilarboppfolging.mappers.VeilarbArenaOppfolging;

import javax.inject.Inject;
import java.util.List;

public class OppfolgingsenhetEndringService {
    @Inject
    private OppfolgingsenhetHistorikkRepository enhetHistorikkRepository;

    public void behandleBrukerEndring(VeilarbArenaOppfolging arenaOppfolging) {
        String aktoerid = arenaOppfolging.getAktoerid();
        String arenaNavKontor = arenaOppfolging.getNav_kontor();
        List<OppfolgingsenhetEndringData> eksisterendeHistorikk = enhetHistorikkRepository.hentOppfolgingsenhetEndringerForAktorId(aktoerid);

        if (eksisterendeHistorikk.isEmpty()) {
            enhetHistorikkRepository.insertOppfolgingsenhetEndringForAktorId(aktoerid, arenaNavKontor);
        } else if (!arenaNavKontor.equals(eksisterendeHistorikk.get(0).getEnhet())) {
            enhetHistorikkRepository.insertOppfolgingsenhetEndringForAktorId(aktoerid, arenaNavKontor);
        }
    }
}
