package no.nav.veilarboppfolging.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.behandle_arbeidssoker.BehandleArbeidssokerClient;
import no.nav.veilarboppfolging.controller.request.Innsatsgruppe;
import no.nav.veilarboppfolging.controller.request.SykmeldtBrukerType;
import no.nav.veilarboppfolging.domain.Oppfolgingsbruker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
public class AktiverBrukerService {

    private final AuthService authService;

    private final BehandleArbeidssokerClient behandleArbeidssokerClient;

    private final OppfolgingService oppfolgingService;

    private final TransactionTemplate transactor;

    @Autowired
    public AktiverBrukerService(
            AuthService authService,
            OppfolgingService oppfolgingService,
            BehandleArbeidssokerClient behandleArbeidssokerClient,
            TransactionTemplate transactor
    ) {
        this.authService = authService;
        this.oppfolgingService = oppfolgingService;
        this.behandleArbeidssokerClient = behandleArbeidssokerClient;
        this.transactor = transactor;
    }

    public void aktiverBruker(Fnr fnr, Innsatsgruppe innsatsgruppe) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);

        transactor.executeWithoutResult((status) -> aktiverBrukerOgOppfolging(fnr, aktorId, innsatsgruppe));
    }

    public void reaktiverBruker(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);

        transactor.executeWithoutResult((status) -> startReaktiveringAvBrukerOgOppfolging(fnr, aktorId));
    }

    private void startReaktiveringAvBrukerOgOppfolging(Fnr fnr, AktorId aktorId) {
        oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(
                Oppfolgingsbruker.reaktivertBruker(aktorId),
                fnr
        );

        behandleArbeidssokerClient.reaktiverBrukerIArena(fnr);
    }

    //  TODO: Innsatsgruppe brukes kun av veilarbdirigent som nå henter ting fra Kafka, kan snart fjernes
    private void aktiverBrukerOgOppfolging(Fnr fnr, AktorId aktorId, Innsatsgruppe innsatsgruppe) {
        oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(
                Oppfolgingsbruker.arbeidssokerOppfolgingsBruker(aktorId, innsatsgruppe), fnr);
        behandleArbeidssokerClient.opprettBrukerIArena(fnr, innsatsgruppe);
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

