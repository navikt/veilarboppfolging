package no.nav.veilarboppfolging.service;

import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.repository.SisteEndringPaaOppfolgingBrukerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Optional;


@Service
public class SisteEndringPaaOppfolgingBrukerService {
    public final SisteEndringPaaOppfolgingBrukerRepository sisteEndringPaaOppfolgingBrukerRepository;

    @Autowired
    public SisteEndringPaaOppfolgingBrukerService(SisteEndringPaaOppfolgingBrukerRepository sisteEndringPaaOppfolgingBrukerRepository) {
        this.sisteEndringPaaOppfolgingBrukerRepository = sisteEndringPaaOppfolgingBrukerRepository;
    }

    public Optional<ZonedDateTime> hentSisteEndringDato(Fnr fnr) {
        return sisteEndringPaaOppfolgingBrukerRepository.hentSisteEndringForFnr(fnr);
    }

    @Transactional
    public int lagreSisteEndring(Fnr fnr, ZonedDateTime endringDato) {
        boolean harVerdi = sisteEndringPaaOppfolgingBrukerRepository.hentSisteEndringForFnr(fnr).isPresent();

        if (harVerdi) {
            return sisteEndringPaaOppfolgingBrukerRepository.oppdatereSisteEndringForFnr(fnr, endringDato);
        }

        return sisteEndringPaaOppfolgingBrukerRepository.insertSisteEndringForFnr(fnr, endringDato);
    }
}
