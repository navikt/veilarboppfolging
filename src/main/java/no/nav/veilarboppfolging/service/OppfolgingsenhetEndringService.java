package no.nav.veilarboppfolging.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarboppfolging.domain.OppfolgingsenhetEndringData;
import no.nav.veilarboppfolging.domain.kafka.VeilarbArenaOppfolgingEndret;
import no.nav.veilarboppfolging.repository.OppfolgingsenhetHistorikkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static no.nav.veilarboppfolging.utils.ArenaUtils.erUnderOppfolging;

@Slf4j
@Service
public class OppfolgingsenhetEndringService {

    private final OppfolgingsenhetHistorikkRepository enhetHistorikkRepository;

    @Autowired
    public OppfolgingsenhetEndringService(OppfolgingsenhetHistorikkRepository enhetHistorikkRepository) {
        this.enhetHistorikkRepository = enhetHistorikkRepository;
    }

    public void behandleBrukerEndring(VeilarbArenaOppfolgingEndret arenaOppfolging) {
        String aktoerid = arenaOppfolging.getAktoerid();
        String arenaNavKontor = arenaOppfolging.getNav_kontor();
        List<OppfolgingsenhetEndringData> eksisterendeHistorikk = enhetHistorikkRepository.hentOppfolgingsenhetEndringerForAktorId(aktoerid);

        if (arenaNavKontor == null || !erUnderOppfolging(arenaOppfolging.getFormidlingsgruppekode(), arenaOppfolging.getKvalifiseringsgruppekode())) {
            log.info(String.format("Legger ikke til historikkinnslag for på aktørid: %s fordi enhet mangler og/eller bruker er ikke under oppfølging", aktoerid));
        }
        else if (eksisterendeHistorikk.isEmpty()) {
            log.info(String.format("Legger til første historikkinnslag for endret oppfolgingsenhet på aktørid: %s", aktoerid));
            enhetHistorikkRepository.insertOppfolgingsenhetEndringForAktorId(aktoerid, arenaNavKontor);
        } else if (!arenaNavKontor.equals(eksisterendeHistorikk.get(0).getEnhet())) {
            log.info(String.format("Legger til historikkinnslag for endret oppfolgingsenhet på aktørid: %s", aktoerid));
            enhetHistorikkRepository.insertOppfolgingsenhetEndringForAktorId(aktoerid, arenaNavKontor);
        }
    }

}
