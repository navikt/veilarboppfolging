package no.nav.veilarboppfolging.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarboppfolging.client.behandle_arbeidssoker.BehandleArbeidssokerClient;
import no.nav.veilarboppfolging.domain.*;
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

    private final KafkaProducerService kafkaProducerService;

    private final NyeBrukereFeedRepository nyeBrukereFeedRepository;

    private final TransactionTemplate transactor;

    @Autowired
    public AktiverBrukerService(
            AuthService authService,
            OppfolgingService oppfolgingService,
            BehandleArbeidssokerClient behandleArbeidssokerClient,
            KafkaProducerService kafkaProducerService,
            NyeBrukereFeedRepository nyeBrukereFeedRepository,
            TransactionTemplate transactor
    ) {
        this.authService = authService;
        this.oppfolgingService = oppfolgingService;
        this.behandleArbeidssokerClient = behandleArbeidssokerClient;
        this.kafkaProducerService = kafkaProducerService;
        this.nyeBrukereFeedRepository = nyeBrukereFeedRepository;
        this.transactor = transactor;
    }

    public void aktiverBruker(AktiverArbeidssokerData bruker) {
        String fnr = ofNullable(bruker.getFnr())
                .map(Fnr::getFnr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "FNR mangler"));

        AktorId aktorId = new AktorId(authService.getAktorIdOrThrow(fnr));

        transactor.executeWithoutResult((status) -> aktiverBrukerOgOppfolging(fnr, aktorId, bruker.getInnsatsgruppe()));
    }

    public void reaktiverBruker(Fnr fnr) {
        AktorId aktorId = new AktorId(authService.getAktorIdOrThrow(fnr.getFnr()));

        transactor.executeWithoutResult((status) -> startReaktiveringAvBrukerOgOppfolging(fnr, aktorId));
    }

    private void startReaktiveringAvBrukerOgOppfolging(Fnr fnr, AktorId aktorId) {
        oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(
                Oppfolgingsbruker.builder()
                        .aktoerId(aktorId.getAktorId())
                        .build()
        );

        behandleArbeidssokerClient.reaktiverBrukerIArena(fnr);
    }

    private void aktiverBrukerOgOppfolging(String fnr, AktorId aktorId, Innsatsgruppe innsatsgruppe) {
        oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(
                Oppfolgingsbruker.builder()
                        .aktoerId(aktorId.getAktorId())
                        .innsatsgruppe(innsatsgruppe)
                        .build());

        behandleArbeidssokerClient.opprettBrukerIArena(fnr, innsatsgruppe);

        nyeBrukereFeedRepository.tryLeggTilFeedIdPaAlleElementerUtenFeedId();
    }

    public void aktiverSykmeldt(String uid, SykmeldtBrukerType sykmeldtBrukerType) {
        transactor.executeWithoutResult((status) -> {
            Oppfolgingsbruker oppfolgingsbruker = Oppfolgingsbruker.builder()
                    .sykmeldtBrukerType(sykmeldtBrukerType)
                    .aktoerId(authService.getAktorIdOrThrow(uid))
                    .build();

            oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(oppfolgingsbruker);
        });
    }

}

