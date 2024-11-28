package no.nav.veilarboppfolging.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2;
import no.nav.veilarboppfolging.repository.OppfolgingsenhetHistorikkRepository;
import no.nav.veilarboppfolging.repository.entity.OppfolgingsenhetEndringEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Optional.ofNullable;
import static no.nav.veilarboppfolging.utils.ArenaUtils.erUnderOppfolging;
import static no.nav.veilarboppfolging.utils.SecureLog.secureLog;

@Slf4j
@Service
public class OppfolgingsenhetEndringService {

    private final OppfolgingsenhetHistorikkRepository enhetHistorikkRepository;

    private final AuthService authService;

    @Autowired
    public OppfolgingsenhetEndringService(OppfolgingsenhetHistorikkRepository enhetHistorikkRepository, AuthService authService) {
        this.enhetHistorikkRepository = enhetHistorikkRepository;
        this.authService = authService;
    }

    public void behandleBrukerEndring(EndringPaaOppfoelgingsBrukerV2 brukerV2) {
        AktorId aktorId = authService.getAktorIdOrThrow(Fnr.of(brukerV2.getFodselsnummer()));
        String arenaNavKontor = brukerV2.getOppfolgingsenhet();

        List<OppfolgingsenhetEndringEntity> eksisterendeHistorikk = enhetHistorikkRepository.hentOppfolgingsenhetEndringerForAktorId(aktorId);

        var formidlingsgruppe = ofNullable(brukerV2.getFormidlingsgruppe()).orElse(null);
        var kvalifiseringsgruppe = ofNullable(brukerV2.getKvalifiseringsgruppe()).orElse(null);

        if (arenaNavKontor == null || !erUnderOppfolging(formidlingsgruppe, kvalifiseringsgruppe)) {
            secureLog.info(String.format("Legger ikke til historikkinnslag for på aktørid: %s fordi enhet mangler og/eller bruker er ikke under oppfølging", aktorId));
        } else if (eksisterendeHistorikk.isEmpty()) {
            secureLog.info(String.format("Legger til første historikkinnslag for endret oppfolgingsenhet på aktørid: %s", aktorId));
            enhetHistorikkRepository.insertOppfolgingsenhetEndringForAktorId(aktorId, arenaNavKontor);
        } else if (!arenaNavKontor.equals(eksisterendeHistorikk.get(0).getEnhet())) {
            secureLog.info(String.format("Legger til historikkinnslag for endret oppfolgingsenhet på aktørid: %s", aktorId));
            enhetHistorikkRepository.insertOppfolgingsenhetEndringForAktorId(aktorId, arenaNavKontor);
        }
    }

}
