package no.nav.fo.veilarboppfolging.services;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarboppfolging.db.OppfolgingsenhetHistorikkRepository;
import no.nav.fo.veilarboppfolging.domain.OppfolgingsenhetEndringData;
import no.nav.fo.veilarboppfolging.mappers.VeilarbArenaOppfolging;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Slf4j
@Component
public class OppfolgingsenhetEndringService {
    private final OppfolgingsenhetHistorikkRepository enhetHistorikkRepository;

    @Inject
    public OppfolgingsenhetEndringService(OppfolgingsenhetHistorikkRepository enhetHistorikkRepository) {
        this.enhetHistorikkRepository = enhetHistorikkRepository;
    }

    public void behandleBrukerEndring(VeilarbArenaOppfolging arenaOppfolging) {
        String aktoerid = arenaOppfolging.getAktoerid();
        String arenaNavKontor = arenaOppfolging.getNav_kontor();
        List<OppfolgingsenhetEndringData> eksisterendeHistorikk = enhetHistorikkRepository.hentOppfolgingsenhetEndringerForAktorId(aktoerid);

        if (eksisterendeHistorikk.isEmpty()) {
            log.info(String.format("Legger til første historikkinnslag for endret oppfolgingsenhet på aktørid: %s"), aktoerid);
            enhetHistorikkRepository.insertOppfolgingsenhetEndringForAktorId(aktoerid, arenaNavKontor);
        } else if (!arenaNavKontor.equals(eksisterendeHistorikk.get(0).getEnhet())) {
            log.info(String.format("Legger til historikkinnslag for endret oppfolgingsenhet på aktørid: %s"), aktoerid);
            enhetHistorikkRepository.insertOppfolgingsenhetEndringForAktorId(aktoerid, arenaNavKontor);
        }
    }
}
