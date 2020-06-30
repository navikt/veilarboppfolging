package no.nav.veilarboppfolging.services;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.veilarboppfolging.client.behandle_arbeidssoker.BehandleArbeidssokerClient;
import no.nav.veilarboppfolging.db.NyeBrukereFeedRepository;
import no.nav.veilarboppfolging.db.OppfolgingRepository;
import no.nav.veilarboppfolging.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.core.Response;

import static java.util.Optional.ofNullable;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static no.nav.veilarboppfolging.utils.FnrUtils.*;

@Slf4j
@Component
public class AktiverBrukerService {

    private final AktorregisterClient aktorregisterClient;
    private final BehandleArbeidssokerClient behandleArbeidssokerClient;

    private final OppfolgingRepository oppfolgingRepository;
    private final NyeBrukereFeedRepository nyeBrukereFeedRepository;

    @Autowired
    public AktiverBrukerService(OppfolgingRepository oppfolgingRepository,
                                AktorregisterClient aktorregisterClient,
                                BehandleArbeidssokerClient behandleArbeidssokerClient,
                                NyeBrukereFeedRepository nyeBrukereFeedRepository
    ) {
        this.aktorregisterClient = aktorregisterClient;
        this.behandleArbeidssokerClient = behandleArbeidssokerClient;
        this.oppfolgingRepository = oppfolgingRepository;
        this.nyeBrukereFeedRepository = nyeBrukereFeedRepository;
    }

    @Transactional
    public void aktiverBruker(AktiverArbeidssokerData bruker) {
        String fnr = ofNullable(bruker.getFnr())
                .map(f -> f.getFnr())
                .orElse("");

        AktorId aktorId = getAktorIdOrElseThrow(aktorregisterClient, fnr);

        aktiverBrukerOgOppfolging(fnr, aktorId, bruker.getInnsatsgruppe());
    }

    @Transactional
    public void reaktiverBruker(Fnr fnr) {

        AktorId aktorId = getAktorIdOrElseThrow(aktorregisterClient, fnr.getFnr());

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
                .aktoerId(getAktorIdOrElseThrow(aktorregisterClient, uid).getAktorId())
                .build();

        oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(oppfolgingsbruker);
    }

    private class ArenaFeilType implements Feil.Type {
        private String feilType;

        public ArenaFeilType(String feilType) {
            this.feilType = feilType;
        }

        @Override
        public String getName() {
            return feilType;
        }

        @Override
        public Response.Status getStatus() {
            return FORBIDDEN;
        }
    }
}

