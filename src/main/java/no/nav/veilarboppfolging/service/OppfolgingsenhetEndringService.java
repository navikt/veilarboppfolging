package no.nav.veilarboppfolging.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV1;
import no.nav.veilarboppfolging.domain.OppfolgingsenhetEndringData;
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

    public void behandleBrukerEndring(EndringPaaOppfoelgingsBrukerV1 arenaOppfolging) {
        AktorId aktorId = AktorId.of(arenaOppfolging.getAktoerid());
        String arenaNavKontor = arenaOppfolging.getNav_kontor();

        List<OppfolgingsenhetEndringData> eksisterendeHistorikk = enhetHistorikkRepository.hentOppfolgingsenhetEndringerForAktorId(aktorId);

        if (arenaNavKontor == null || !erUnderOppfolging(arenaOppfolging.getFormidlingsgruppekode(), arenaOppfolging.getKvalifiseringsgruppekode())) {
            log.info(String.format("Legger ikke til historikkinnslag for på aktørid: %s fordi enhet mangler og/eller bruker er ikke under oppfølging", aktorId));
        }
        else if (eksisterendeHistorikk.isEmpty()) {
            log.info(String.format("Legger til første historikkinnslag for endret oppfolgingsenhet på aktørid: %s", aktorId));
            enhetHistorikkRepository.insertOppfolgingsenhetEndringForAktorId(aktorId, arenaNavKontor);
        } else if (!arenaNavKontor.equals(eksisterendeHistorikk.get(0).getEnhet())) {
            log.info(String.format("Legger til historikkinnslag for endret oppfolgingsenhet på aktørid: %s", aktorId));
            enhetHistorikkRepository.insertOppfolgingsenhetEndringForAktorId(aktorId, arenaNavKontor);
        }
    }

}
