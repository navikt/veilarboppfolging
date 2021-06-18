package no.nav.veilarboppfolging.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.dkif.DkifClient;
import no.nav.veilarboppfolging.client.dkif.DkifKontaktinfo;
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbArenaOppfolging;
import no.nav.veilarboppfolging.domain.KodeverkBruker;
import no.nav.veilarboppfolging.domain.ManuellStatus;
import no.nav.veilarboppfolging.domain.OppfolgingTable;
import no.nav.veilarboppfolging.repository.ManuellStatusRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.ZonedDateTime;

import static no.nav.veilarboppfolging.domain.KodeverkBruker.SYSTEM;

@Slf4j
@Service
public class ManuellStatusService {

    private final AuthService authService;

    private final ManuellStatusRepository manuellStatusRepository;

    private final ArenaOppfolgingService arenaOppfolgingService;

    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;

    private final DkifClient dkifClient;

    private final KafkaProducerService kafkaProducerService;

    private final TransactionTemplate transactor;

    @Autowired
    public ManuellStatusService(
            AuthService authService,
            ManuellStatusRepository manuellStatusRepository,
            ArenaOppfolgingService arenaOppfolgingService,
            OppfolgingsStatusRepository oppfolgingsStatusRepository,
            DkifClient dkifClient,
            KafkaProducerService kafkaProducerService,
            TransactionTemplate transactor
    ) {
        this.authService = authService;
        this.manuellStatusRepository = manuellStatusRepository;
        this.arenaOppfolgingService = arenaOppfolgingService;
        this.oppfolgingsStatusRepository = oppfolgingsStatusRepository;
        this.dkifClient = dkifClient;
        this.kafkaProducerService = kafkaProducerService;
        this.transactor = transactor;
    }

    public boolean erManuell(AktorId aktorId) {
        return manuellStatusRepository.hentSisteManuellStatus(aktorId)
                .map(ManuellStatus::isManuell)
                .orElse(false);
    }

    /**
     * Gjør en sjekk i DKIF om bruker er reservert.
     * Hvis bruker er reservert så sett manuell status på bruker hvis det ikke allerede er gjort.
     * Bruker må være under oppfølging for å oppdatere manuell status.
     * @param fnr fnr/dnr til bruker som det skal sjekkes på
     */
    public void synkroniserManuellStatusMedDkif(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);

        // Bruker er allerede manuell, trenger ikke å sjekke i DKIF
        if (erManuell(aktorId)) {
            return;
        }

        DkifKontaktinfo dkifKontaktinfo = dkifClient.hentKontaktInfo(fnr);

        if (dkifKontaktinfo.isReservert()) {
            var manuellStatus = new ManuellStatus()
                    .setAktorId(aktorId.get())
                    .setManuell(true)
                    .setDato(ZonedDateTime.now())
                    .setBegrunnelse("Brukeren er reservert i Kontakt- og reservasjonsregisteret")
                    .setOpprettetAv(SYSTEM);

            log.info("Bruker er reservert i KRR, setter bruker aktorId={} til manuell", aktorId);
            oppdaterManuellStatus(aktorId, manuellStatus);
        }
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

        OppfolgingTable oppfolging = oppfolgingsStatusRepository.fetch(aktorId);
        DkifKontaktinfo kontaktinfo = dkifClient.hentKontaktInfo(fnr);

        boolean erUnderOppfolging = oppfolging.isUnderOppfolging();
        boolean gjeldendeErManuell = erManuell(aktorId);
        boolean reservertIKrr = kontaktinfo.isReservert();

        if (erUnderOppfolging && (gjeldendeErManuell != manuell) && (!reservertIKrr || manuell)) {
            val nyStatus = new ManuellStatus()
                    .setAktorId(aktorId.get())
                    .setManuell(manuell)
                    .setDato(ZonedDateTime.now())
                    .setBegrunnelse(begrunnelse)
                    .setOpprettetAv(opprettetAv)
                    .setOpprettetAvBrukerId(opprettetAvBrukerId);

            oppdaterManuellStatus(aktorId, nyStatus);
        }
    }

    private void oppdaterManuellStatus(AktorId aktorId, ManuellStatus manuellStatus) {
        transactor.executeWithoutResult((ignored) -> {
            manuellStatusRepository.create(manuellStatus);
            kafkaProducerService.publiserEndringPaManuellStatus(aktorId, manuellStatus.isManuell());
        });
    }

}
