package no.nav.veilarboppfolging.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.request.SykmeldtBrukerType;
import no.nav.veilarboppfolging.domain.Oppfolgingsbruker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
public class AktiverBrukerService {

    private final AuthService authService;

    private final OppfolgingService oppfolgingService;

    private final TransactionTemplate transactor;

    @Autowired
    public AktiverBrukerService(
            AuthService authService,
            OppfolgingService oppfolgingService,
            TransactionTemplate transactor
    ) {
        this.authService = authService;
        this.oppfolgingService = oppfolgingService;
        this.transactor = transactor;
    }

    //  TODO: SykmeldtBrukerType brukes kun av veilarbdirigent som nå henter ting fra Kafka, kan snart fjernes
    public void aktiverSykmeldt(Fnr fnr, SykmeldtBrukerType sykmeldtBrukerType) {
        transactor.executeWithoutResult((status) -> {
            var aktorId = authService.getAktorIdOrThrow(fnr);
            var oppfolgingsbruker = Oppfolgingsbruker.sykmeldtMerOppfolgingsBruker(aktorId, sykmeldtBrukerType);
            oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(oppfolgingsbruker, fnr);
        });
    }

}

