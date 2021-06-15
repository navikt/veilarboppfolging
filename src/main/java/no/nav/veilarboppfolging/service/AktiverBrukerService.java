package no.nav.veilarboppfolging.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.behandle_arbeidssoker.BehandleArbeidssokerClient;
import no.nav.veilarboppfolging.controller.request.AktiverArbeidssokerData;
import no.nav.veilarboppfolging.controller.request.SykmeldtBrukerType;
import no.nav.veilarboppfolging.domain.Innsatsgruppe;
import no.nav.veilarboppfolging.domain.Oppfolgingsbruker;
import no.nav.veilarboppfolging.repository.NyeBrukereFeedRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import static java.util.Optional.ofNullable;

@Slf4j
@Service
public class AktiverBrukerService {

    private final AuthService authService;

    private final BehandleArbeidssokerClient behandleArbeidssokerClient;

    private final OppfolgingService oppfolgingService;

    private final NyeBrukereFeedRepository nyeBrukereFeedRepository;

    private final TransactionTemplate transactor;

    @Autowired
    public AktiverBrukerService(
            AuthService authService,
            OppfolgingService oppfolgingService,
            BehandleArbeidssokerClient behandleArbeidssokerClient,
            NyeBrukereFeedRepository nyeBrukereFeedRepository,
            TransactionTemplate transactor
    ) {
        this.authService = authService;
        this.oppfolgingService = oppfolgingService;
        this.behandleArbeidssokerClient = behandleArbeidssokerClient;
        this.nyeBrukereFeedRepository = nyeBrukereFeedRepository;
        this.transactor = transactor;
    }

    public void aktiverBruker(AktiverArbeidssokerData bruker) {
        no.nav.veilarboppfolging.controller.request.Fnr requestFnr = ofNullable(bruker.getFnr())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "FNR mangler"));

        Fnr fnr = Fnr.of(requestFnr.getFnr());

        AktorId aktorId = authService.getAktorIdOrThrow(fnr);

        transactor.executeWithoutResult((status) -> aktiverBrukerOgOppfolging(fnr, aktorId, bruker.getInnsatsgruppe()));
    }

    public void reaktiverBruker(Fnr fnr) {
        AktorId aktorId = new AktorId(authService.getAktorIdOrThrow(fnr.get()));

        transactor.executeWithoutResult((status) -> startReaktiveringAvBrukerOgOppfolging(fnr, aktorId));
    }

    private void startReaktiveringAvBrukerOgOppfolging(Fnr fnr, AktorId aktorId) {
        oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(
                Oppfolgingsbruker.builder()
                        .aktoerId(aktorId.get())
                        .build()
        );

        behandleArbeidssokerClient.reaktiverBrukerIArena(fnr);
    }

    private void aktiverBrukerOgOppfolging(Fnr fnr, AktorId aktorId, Innsatsgruppe innsatsgruppe) {
        oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(
                Oppfolgingsbruker.builder()
                        .aktoerId(aktorId.get())
                        .innsatsgruppe(innsatsgruppe)
                        .build());

        behandleArbeidssokerClient.opprettBrukerIArena(fnr, innsatsgruppe);

        nyeBrukereFeedRepository.tryLeggTilFeedIdPaAlleElementerUtenFeedId();
    }

    public void aktiverSykmeldt(Fnr fnr, SykmeldtBrukerType sykmeldtBrukerType) {
        transactor.executeWithoutResult((status) -> {
            Oppfolgingsbruker oppfolgingsbruker = Oppfolgingsbruker.builder()
                    .sykmeldtBrukerType(sykmeldtBrukerType)
                    .aktoerId(authService.getAktorIdOrThrow(fnr).get())
                    .build();

            oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(oppfolgingsbruker);
        });
    }

}

