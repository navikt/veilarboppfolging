package no.nav.veilarboppfolging.service;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.veilarboppfolging.domain.Fnr;
import no.nav.veilarboppfolging.domain.KodeverkBruker;
import no.nav.veilarboppfolging.domain.ManuellStatus;
import no.nav.veilarboppfolging.domain.OppfolgingTable;
import no.nav.veilarboppfolging.kafka.OppfolgingStatusKafkaProducer;
import no.nav.veilarboppfolging.repository.ManuellStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Optional;

import static java.lang.System.currentTimeMillis;

@Service
public class ManuellStatusService {

    private final AuthService authService;

    private final OppfolgingStatusKafkaProducer oppfolgingStatusKafkaProducer;

    private final ManuellStatusRepository manuellStatusRepository;

    @Autowired
    public ManuellStatusService(
            AuthService authService,
            OppfolgingStatusKafkaProducer oppfolgingStatusKafkaProducer,
            ManuellStatusRepository manuellStatusRepository
    ) {
        this.authService = authService;
        this.oppfolgingStatusKafkaProducer = oppfolgingStatusKafkaProducer;
        this.manuellStatusRepository = manuellStatusRepository;
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

        //        TODO: Mangler sjekk på tilgang til enhet
//        val resolver = sjekkTilgangTilEnhet(fnr);

        // TODO: retrieve booleans
        boolean erUnderOppfolging = false;
        boolean gjeldendeErManuell = true;
        boolean reservertIKrr = false;

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

        oppfolgingStatusKafkaProducer.send(new Fnr(fnr));
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
