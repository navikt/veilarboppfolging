package no.nav.veilarboppfolging.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.dkif.DkifClient;
import no.nav.veilarboppfolging.client.dkif.DkifKontaktinfo;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;
import no.nav.veilarboppfolging.repository.ManuellStatusRepository;
import no.nav.veilarboppfolging.repository.entity.ManuellStatusEntity;
import no.nav.veilarboppfolging.repository.enums.KodeverkBruker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static no.nav.veilarboppfolging.repository.enums.KodeverkBruker.SYSTEM;

@Slf4j
@Service
public class ManuellStatusService {

    private final AuthService authService;

    private final ManuellStatusRepository manuellStatusRepository;

    private final ArenaOppfolgingService arenaOppfolgingService;

    private final OppfolgingService oppfolgingService;

    private final DkifClient dkifClient;

    private final KafkaProducerService kafkaProducerService;

    private final TransactionTemplate transactor;

    @Autowired
    public ManuellStatusService(
            AuthService authService,
            ManuellStatusRepository manuellStatusRepository,
            ArenaOppfolgingService arenaOppfolgingService,
            OppfolgingService oppfolgingService,
            DkifClient dkifClient,
            KafkaProducerService kafkaProducerService,
            TransactionTemplate transactor
    ) {
        this.authService = authService;
        this.manuellStatusRepository = manuellStatusRepository;
        this.arenaOppfolgingService = arenaOppfolgingService;
        this.oppfolgingService = oppfolgingService;
        this.dkifClient = dkifClient;
        this.kafkaProducerService = kafkaProducerService;
        this.transactor = transactor;
    }

    public boolean erManuell(AktorId aktorId) {
        return manuellStatusRepository.hentSisteManuellStatus(aktorId)
                .map(ManuellStatusEntity::isManuell)
                .orElse(false);
    }

    public Optional<ManuellStatusEntity> hentManuellStatus(long manuellStatusId) {
        return manuellStatusRepository.hentManuellStatus(manuellStatusId);
    }

    public List<ManuellStatusEntity> hentManuellStatusHistorikk(AktorId aktorId) {
        return manuellStatusRepository.history(aktorId);
    }

    /**
     * Gjør en sjekk i DKIF om bruker er reservert.
     * Hvis bruker er reservert så sett manuell status på bruker hvis det ikke allerede er gjort.
     * Bruker må være under oppfølging for å oppdatere manuell status.
     * @param fnr fnr/dnr til bruker som det skal sjekkes på
     */
    public void synkroniserManuellStatusMedDkif(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);

        boolean erUnderOppfolging = oppfolgingService.erUnderOppfolging(aktorId);

        if (!erUnderOppfolging) {
            return;
        }

        DkifKontaktinfo dkifKontaktinfo = hentDkifKontaktinfo(fnr);

        if (dkifKontaktinfo.isReservert()) {
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

        log.info("Bruker er reservert i KRR, setter bruker aktorId={} til manuell", aktorId);
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

        DkifKontaktinfo kontaktinfo = hentDkifKontaktinfo(fnr);

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

    public DkifKontaktinfo hentDkifKontaktinfo(Fnr fnr){
        return dkifClient.hentKontaktInfo(fnr)
                .orElseGet(() -> new DkifKontaktinfo()
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
