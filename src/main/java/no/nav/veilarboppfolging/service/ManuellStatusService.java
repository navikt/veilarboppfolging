package no.nav.veilarboppfolging.service;

import lombok.SneakyThrows;
import lombok.val;
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

import java.sql.Timestamp;
import java.util.Optional;

import static java.lang.System.currentTimeMillis;

@Service
public class ManuellStatusService {

    private final AuthService authService;

    private final ManuellStatusRepository manuellStatusRepository;

    private final ArenaOppfolgingService arenaOppfolgingService;

    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;

    private final DkifClient dkifClient;

    private final KafkaProducerService kafkaProducerService;

    @Autowired
    public ManuellStatusService(
            AuthService authService,
            ManuellStatusRepository manuellStatusRepository,
            ArenaOppfolgingService arenaOppfolgingService,
            OppfolgingsStatusRepository oppfolgingsStatusRepository,
            DkifClient dkifClient, KafkaProducerService kafkaProducerService) {
        this.authService = authService;
        this.manuellStatusRepository = manuellStatusRepository;
        this.arenaOppfolgingService = arenaOppfolgingService;
        this.oppfolgingsStatusRepository = oppfolgingsStatusRepository;
        this.dkifClient = dkifClient;
        this.kafkaProducerService = kafkaProducerService;
    }

    @SneakyThrows
    public void settDigitalBruker(String fnr) {
        String aktorId = authService.getAktorIdOrThrow(fnr);
        oppdaterManuellStatus(fnr, false, "Brukeren endret til digital oppfølging", KodeverkBruker.EKSTERN, aktorId);
    }

    @SneakyThrows
    public void oppdaterManuellStatus(String fnr, boolean manuell, String begrunnelse, KodeverkBruker opprettetAv, String opprettetAvBrukerId) {
        String aktorId = authService.getAktorIdOrThrow(fnr);
        authService.sjekkLesetilgangMedAktorId(aktorId);

        VeilarbArenaOppfolging arenaOppfolging = arenaOppfolgingService.hentOppfolgingFraVeilarbarena(fnr).orElseThrow();

        authService.sjekkTilgangTilEnhet(arenaOppfolging.getNav_kontor());

        OppfolgingTable oppfolging = oppfolgingsStatusRepository.fetch(aktorId);
        DkifKontaktinfo kontaktinfo = dkifClient.hentKontaktInfo(fnr);

        boolean erUnderOppfolging = oppfolging.isUnderOppfolging();
        boolean gjeldendeErManuell = erManuell(oppfolging);
        boolean reservertIKrr = kontaktinfo.isReservert();

        if (erUnderOppfolging && (gjeldendeErManuell != manuell) && (!reservertIKrr || manuell)) {
            val nyStatus = new ManuellStatus()
                    .setAktorId(aktorId)
                    .setManuell(manuell)
                    .setDato(new Timestamp(currentTimeMillis()))
                    .setBegrunnelse(begrunnelse)
                    .setOpprettetAv(opprettetAv)
                    .setOpprettetAvBrukerId(opprettetAvBrukerId);

            manuellStatusRepository.create(nyStatus);
        }

        kafkaProducerService.publiserOppfolgingStatusEndret(aktorId);
    }

    public boolean erManuell (OppfolgingTable eksisterendeOppfolgingstatus) {
        return Optional.ofNullable(eksisterendeOppfolgingstatus)
                .map(oppfolgingstatus ->  {
                    long gjeldendeManuellStatusId = eksisterendeOppfolgingstatus.getGjeldendeManuellStatusId();
                    return gjeldendeManuellStatusId > 0 && manuellStatusRepository.fetch(gjeldendeManuellStatusId).isManuell();
                })
                .orElse(false);
    }

}
