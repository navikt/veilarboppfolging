package no.nav.veilarboppfolging.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.EnhetId;
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.EndringPaaOppfolgingsBruker;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsenhetHistorikkRepository;
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity;
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
    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;

    @Autowired
    public OppfolgingsenhetEndringService(OppfolgingsenhetHistorikkRepository enhetHistorikkRepository, OppfolgingsStatusRepository oppfolgingsStatusRepository) {
        this.enhetHistorikkRepository = enhetHistorikkRepository;
        this.oppfolgingsStatusRepository = oppfolgingsStatusRepository;
    }

    public void behandleBrukerEndring(EndringPaaOppfolgingsBruker brukerV2) {
        String enhetString = brukerV2.getOppfolgingsenhet();
        if (enhetString == null || enhetString.isBlank()) {
            return;
        }
        EnhetId arenaNavKontor = EnhetId.of(enhetString);

        List<OppfolgingsenhetEndringEntity> eksisterendeHistorikk = enhetHistorikkRepository.hentOppfolgingsenhetEndringerForAktorId(brukerV2.getAktorId());

        var formidlingsgruppe = ofNullable(brukerV2.getFormidlingsgruppe()).orElse(null);
        var kvalifiseringsgruppe = ofNullable(brukerV2.getKvalifiseringsgruppe()).orElse(null);

        var erUnderOppfolgingLokalt = oppfolgingsStatusRepository.hentOppfolging(brukerV2.getAktorId())
                .map(OppfolgingEntity::isUnderOppfolging)
                .orElse(false);
        var erUnderOppfolgingIArena = erUnderOppfolging(formidlingsgruppe, kvalifiseringsgruppe);
        var erUnderOppfolging = erUnderOppfolgingIArena || erUnderOppfolgingLokalt;

        if (arenaNavKontor.get() == null || !erUnderOppfolging) {
            secureLog.info(String.format("Legger ikke til historikkinnslag for på aktørid: %s fordi enhet mangler og/eller bruker er ikke under oppfølging", brukerV2.getAktorId()));
        } else if (eksisterendeHistorikk.isEmpty()) {
            secureLog.info(String.format("Legger til første historikkinnslag for endret oppfolgingsenhet på aktørid: %s", brukerV2.getAktorId()));
            enhetHistorikkRepository.insertOppfolgingsenhetEndringForAktorId(brukerV2.getAktorId(), arenaNavKontor);
        } else if (!arenaNavKontor.get().equals(eksisterendeHistorikk.get(0).getEnhet())) {
            secureLog.info(String.format("Legger til historikkinnslag for endret oppfolgingsenhet på aktørid: %s", brukerV2.getAktorId()));
            enhetHistorikkRepository.insertOppfolgingsenhetEndringForAktorId(brukerV2.getAktorId(), arenaNavKontor);
        }
    }

}
