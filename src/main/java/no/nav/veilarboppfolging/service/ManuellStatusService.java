package no.nav.veilarboppfolging.service;

import lombok.SneakyThrows;
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

import java.time.ZonedDateTime;
import java.util.Optional;

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
    public void settDigitalBruker(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);
        oppdaterManuellStatus(fnr, false, "Brukeren endret til digital oppfølging", KodeverkBruker.EKSTERN, aktorId.get());
    }

    @SneakyThrows
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
        boolean gjeldendeErManuell = erManuell(oppfolging);
        boolean reservertIKrr = kontaktinfo.isReservert();

        if (erUnderOppfolging && (gjeldendeErManuell != manuell) && (!reservertIKrr || manuell)) {
            val nyStatus = new ManuellStatus()
                    .setAktorId(aktorId.get())
                    .setManuell(manuell)
                    .setDato(ZonedDateTime.now())
                    .setBegrunnelse(begrunnelse)
                    .setOpprettetAv(opprettetAv)
                    .setOpprettetAvBrukerId(opprettetAvBrukerId);

            manuellStatusRepository.create(nyStatus);
            kafkaProducerService.publiserEndringPaManuellStatus(aktorId, manuell);
        }

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
