package no.nav.veilarboppfolging.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.veilarboppfolging.client.behandle_arbeidssoker.BehandleArbeidssokerClient;
import no.nav.veilarboppfolging.domain.*;
import no.nav.veilarboppfolging.repository.NyeBrukereFeedRepository;
import no.nav.veilarboppfolging.repository.OppfolgingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Optional.ofNullable;

@Slf4j
@Service
public class AktiverBrukerService {

    private final AuthService authService;

    private final BehandleArbeidssokerClient behandleArbeidssokerClient;

    private final OppfolgingRepository oppfolgingRepository;

    private final NyeBrukereFeedRepository nyeBrukereFeedRepository;

    @Autowired
    public AktiverBrukerService(
            AuthService authService,
            OppfolgingRepository oppfolgingRepository,
            BehandleArbeidssokerClient behandleArbeidssokerClient,
            NyeBrukereFeedRepository nyeBrukereFeedRepository
    ) {
        this.authService = authService;
        this.oppfolgingRepository = oppfolgingRepository;
        this.behandleArbeidssokerClient = behandleArbeidssokerClient;
        this.nyeBrukereFeedRepository = nyeBrukereFeedRepository;
    }

    @Transactional
    public void aktiverBruker(AktiverArbeidssokerData bruker) {
        String fnr = ofNullable(bruker.getFnr())
                .map(Fnr::getFnr)
                .orElse("");

        AktorId aktorId = new AktorId(authService.getAktorIdOrThrow(fnr));

        aktiverBrukerOgOppfolging(fnr, aktorId, bruker.getInnsatsgruppe());
    }

    @Transactional
    public void reaktiverBruker(Fnr fnr) {
        AktorId aktorId = new AktorId(authService.getAktorIdOrThrow(fnr.getFnr()));
        startReaktiveringAvBrukerOgOppfolging(fnr, aktorId);
    }

    private void startReaktiveringAvBrukerOgOppfolging(Fnr fnr, AktorId aktorId) {
        oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(
                Oppfolgingsbruker.builder()
                        .aktoerId(aktorId.getAktorId())
                        .build()
        );

        behandleArbeidssokerClient.reaktiverBrukerIArena(fnr);
    }

    private void aktiverBrukerOgOppfolging(String fnr, AktorId aktorId, Innsatsgruppe innsatsgruppe) {

        oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(
                Oppfolgingsbruker.builder()
                        .aktoerId(aktorId.getAktorId())
                        .innsatsgruppe(innsatsgruppe)
                        .build()
        );

        behandleArbeidssokerClient.opprettBrukerIArena(fnr, innsatsgruppe);

        nyeBrukereFeedRepository.tryLeggTilFeedIdPaAlleElementerUtenFeedId();
    }

    @Transactional
    public void aktiverSykmeldt(String uid, SykmeldtBrukerType sykmeldtBrukerType) {
        Oppfolgingsbruker oppfolgingsbruker = Oppfolgingsbruker.builder()
                .sykmeldtBrukerType(sykmeldtBrukerType)
                .aktoerId(authService.getAktorIdOrThrow(uid))
                .build();

        oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(oppfolgingsbruker);
    }

}

