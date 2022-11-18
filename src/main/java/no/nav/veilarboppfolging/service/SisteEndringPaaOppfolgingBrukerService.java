package no.nav.veilarboppfolging.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.repository.SisteEndringPaaOppfolgingBrukerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SisteEndringPaaOppfolgingBrukerService {
    public final SisteEndringPaaOppfolgingBrukerRepository sisteEndringPaaOppfolgingBrukerRepository;

    public Optional<ZonedDateTime> hentSisteEndringDato(Fnr fnr) {
        return sisteEndringPaaOppfolgingBrukerRepository.hentSisteEndringForFnr(fnr);
    }

    @Transactional
    public int lagreSisteEndring(Fnr fnr, ZonedDateTime endringDato) {
        Optional<ZonedDateTime> sisteEndringForFnrOptional = sisteEndringPaaOppfolgingBrukerRepository.hentSisteEndringForFnr(fnr);

        if (sisteEndringForFnrOptional.isPresent()) {
            return oppdatereSisteEndring(fnr, endringDato, sisteEndringForFnrOptional.get());
        }

        return sisteEndringPaaOppfolgingBrukerRepository.insertSisteEndringForFnr(fnr, endringDato);
    }

    private int oppdatereSisteEndring(Fnr fnr, ZonedDateTime endringDato, ZonedDateTime sisteLagretEndring) {
        if (sisteLagretEndring.isBefore(endringDato)) {
            return sisteEndringPaaOppfolgingBrukerRepository.oppdatereSisteEndringForFnr(fnr, endringDato);
        }
        return 0;
    }

}
