package no.nav.veilarboppfolging.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.digdir_krr.KRRData;
import no.nav.veilarboppfolging.client.digdir_krr.DigdirClient;
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService;
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

        KRRData digdirKontaktinfo = hentDigdirKontaktinfo(fnr);

        if (digdirKontaktinfo.isReservert()) {
            settBrukerTilManuellGrunnetReservertIKRR(aktorId);
        }
    }

    public void settBrukerTilManuellGrunnetReservertIKRR(AktorId aktorId) {
        // Hvis bruker allerede er manuell så trenger vi ikke å sette status på nytt
        if (erManuell(aktorId)) {
            log.info("Bruker er allerede manuell og trenger ikke synkroniseres med KRR");
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

    public void settBrukerTilDigitalGrunnetIkkeReservertIKRR(AktorId aktorId) {
        // Hvis bruker allerede er digital så trenger vi ikke å sette status på nytt
        if (!erManuell(aktorId)) {
            log.info("Bruker er allerede digital og trenger ikke synkroniseres med KRR");
            return;
        }

        var manuellStatus = new ManuellStatusEntity()
                .setAktorId(aktorId.get())
                .setManuell(false)
                .setDato(ZonedDateTime.now())
                .setBegrunnelse("Brukeren er ikke lenger reservert i Kontakt- og reservasjonsregisteret")
                .setOpprettetAv(SYSTEM);

        secureLog.info("Bruker er ikke lenger reservert i KRR, setter bruker aktorId={} til digital", aktorId);
        oppdaterManuellStatus(aktorId, manuellStatus);
    }

    public void settDigitalBruker(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);
        oppdaterManuellStatus(fnr, false, "Brukeren endret til digital oppfølging", KodeverkBruker.EKSTERN, aktorId.get());
    }

    public void oppdaterManuellStatus(Fnr fnr, boolean manuell, String begrunnelse, KodeverkBruker opprettetAv, String opprettetAvBrukerId) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);
        authService.sjekkLesetilgangMedAktorId(aktorId);

        if (!authService.erEksternBruker()) {
            var enhet = Optional.ofNullable(arenaOppfolgingService.hentArenaOppfolgingsEnhetId(fnr)).orElseThrow();
            authService.sjekkTilgangTilEnhet(enhet.get());
        }

        KRRData kontaktinfo = hentDigdirKontaktinfo(fnr);

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

    public KRRData hentDigdirKontaktinfo(Fnr fnr) {
        return digdirClient.hentKontaktInfo(fnr)
                .orElseGet(() -> new KRRData()
                        .withPersonident(fnr.get())
                        .withAktiv(false)
                        .withKanVarsles(false)
                        .withReservert(false));
    }
    private void oppdaterManuellStatus(AktorId aktorId, ManuellStatusEntity manuellStatus) {
        transactor.executeWithoutResult((ignored) -> {
            manuellStatusRepository.create(manuellStatus);
            kafkaProducerService.publiserEndringPaManuellStatus(aktorId, manuellStatus.isManuell());
        });
    }

}
