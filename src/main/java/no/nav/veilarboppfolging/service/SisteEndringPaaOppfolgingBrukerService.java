package no.nav.veilarboppfolging.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.repository.SisteEndringPaaOppfolgingBrukerRepository;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SisteEndringPaaOppfolgingBrukerService {
    public final SisteEndringPaaOppfolgingBrukerRepository sisteEndringPaaOppfolgingBrukerRepository;

    public Optional<ZonedDateTime> hentSisteEndringDato(Fnr fnr){
        return sisteEndringPaaOppfolgingBrukerRepository.hentSisteEndringForFnr(fnr);
    }

    public int lagreSisteEndring(Fnr fnr, ZonedDateTime endringDato){
        boolean harVerdi = sisteEndringPaaOppfolgingBrukerRepository.hentSisteEndringForFnr(fnr).isPresent();

        if (harVerdi){
            return sisteEndringPaaOppfolgingBrukerRepository.oppdatereSisteEndringForFnr(fnr, endringDato);
        }

        return sisteEndringPaaOppfolgingBrukerRepository.insertSisteEndringForFnr(fnr, endringDato);
    }

}
