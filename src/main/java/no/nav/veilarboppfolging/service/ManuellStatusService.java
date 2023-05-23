package no.nav.veilarboppfolging.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;
import no.nav.veilarboppfolging.client.digdir_krr.DigdirClient;
import no.nav.veilarboppfolging.client.digdir_krr.DigdirKontaktinfo;
import no.nav.veilarboppfolging.repository.ManuellStatusRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.repository.entity.ManuellStatusEntity;
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity;
import no.nav.veilarboppfolging.repository.enums.KodeverkBruker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static no.nav.veilarboppfolging.repository.enums.KodeverkBruker.SYSTEM;
import static no.nav.veilarboppfolging.utils.SecureLog.secureLog;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManuellStatusService {

    private final AuthService authService;

    private final ManuellStatusRepository manuellStatusRepository;

    private final ArenaOppfolgingService arenaOppfolgingService;

    private final OppfolgingService oppfolgingService;

    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;

    private final DigdirClient digdirClient;

    private final KafkaProducerService kafkaProducerService;

    private final TransactionTemplate transactor;

    public Optional<ManuellStatusEntity> hentManuellStatus(AktorId aktorId) {
        Long manuellStatusId = oppfolgingsStatusRepository.hentOppfolging(aktorId)
                .map(OppfolgingEntity::getGjeldendeManuellStatusId)
                .orElse(null);

        if (manuellStatusId == null) {
            return Optional.empty();
        }

        return manuellStatusRepository.hentManuellStatus(manuellStatusId);
    }

    public boolean erManuell(AktorId aktorId) {
        return hentManuellStatus(aktorId)
                .map(ManuellStatusEntity::isManuell)
                .orElse(false);
    }

    public boolean erManuell(Fnr fnr) {
        return erManuell(authService.getAktorIdOrThrow(fnr));
    }

    public List<ManuellStatusEntity> hentManuellStatusHistorikk(AktorId aktorId) {
        return manuellStatusRepository.history(aktorId);
    }

    /**
     * Gjør en sjekk i DIGDIR om bruker er reservert.
     * Hvis bruker er reservert så sett manuell status på bruker hvis det ikke allerede er gjort.
     * Bruker må være under oppfølging for å oppdatere manuell status.
     *
     * @param fnr fnr/dnr til bruker som det skal sjekkes på
     */

    public void synkroniserManuellStatusMedDigdir(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);

        boolean erUnderOppfolging = oppfolgingService.erUnderOppfolging(aktorId);

        if (!erUnderOppfolging) {
            return;
        }

        DigdirKontaktinfo digdirKontaktinfo = hentDigdirKontaktinfo(fnr);

        if (digdirKontaktinfo.isReservert()) {
            settBrukerTilManuellGrunnetReservasjonIKRR(aktorId);
        }
    }

    public void settBrukerTilManuellGrunnetReservasjonIKRR(AktorId aktorId) {
        // Hvis bruker allerede er manuell så trenger vi ikke å sette status på nytt
        if (erManuell(aktorId)) {
            log.info("Bruker er allerede manuell og trenger ikke å oppdateres med reservasjon fra KRR");
            return;
        }

        var manuellStatus = new ManuellStatusEntity()
                .setAktorId(aktorId.get())
                .setManuell(true)
                .setDato(ZonedDateTime.now())
                .setBegrunnelse("Brukeren er reservert i Kontakt- og reservasjonsregisteret")
                .setOpprettetAv(SYSTEM);

        secureLog.info("Bruker er reservert i KRR, setter bruker aktorId={} til manuell", aktorId);
        oppdaterManuellStatus(aktorId, manuellStatus);
    }

    public void settDigitalBruker(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);
        oppdaterManuellStatus(fnr, false, "Brukeren endret til digital oppfølging", KodeverkBruker.EKSTERN, aktorId.get());
    }

    public void oppdaterManuellStatus(Fnr fnr, boolean manuell, String begrunnelse, KodeverkBruker opprettetAv, String opprettetAvBrukerId) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);
        authService.sjekkLesetilgangMedAktorId(aktorId);

        VeilarbArenaOppfolging arenaOppfolging = arenaOppfolgingService.hentOppfolgingFraVeilarbarena(fnr).orElseThrow();

        if (!authService.erEksternBruker()) {
            authService.sjekkTilgangTilEnhet(arenaOppfolging.getNav_kontor());
        }

        DigdirKontaktinfo kontaktinfo = hentDigdirKontaktinfo(fnr);

        boolean erUnderOppfolging = oppfolgingService.erUnderOppfolging(aktorId);
        boolean gjeldendeErManuell = erManuell(aktorId);
        boolean reservertIKrr = kontaktinfo.isReservert();

        if (erUnderOppfolging && (gjeldendeErManuell != manuell) && (!reservertIKrr || manuell)) {
            val nyStatus = new ManuellStatusEntity()
                    .setAktorId(aktorId.get())
                    .setManuell(manuell)
                    .setDato(ZonedDateTime.now())
                    .setBegrunnelse(begrunnelse)
                    .setOpprettetAv(opprettetAv)
                    .setOpprettetAvBrukerId(opprettetAvBrukerId);

            oppdaterManuellStatus(aktorId, nyStatus);
        }
    }

    public DigdirKontaktinfo hentDigdirKontaktinfo(Fnr fnr) {
        return digdirClient.hentKontaktInfo(fnr)
                .orElseGet(() -> new DigdirKontaktinfo()
                        .setPersonident(fnr.get())
                        .setKanVarsles(true)
                        .setReservert(false));
    }
    private void oppdaterManuellStatus(AktorId aktorId, ManuellStatusEntity manuellStatus) {
        transactor.executeWithoutResult((ignored) -> {
            manuellStatusRepository.create(manuellStatus);
            kafkaProducerService.publiserEndringPaManuellStatus(aktorId, manuellStatus.isManuell());
        });
    }

}
