package no.nav.veilarboppfolging.service;

import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.binding.*;
import no.nav.veilarboppfolging.client.behandle_arbeidssoker.BehandleArbeidssokerClient;
import no.nav.veilarboppfolging.domain.AktiverArbeidssokerData;
import no.nav.veilarboppfolging.domain.Fnr;
import no.nav.veilarboppfolging.domain.Innsatsgruppe;
import no.nav.veilarboppfolging.repository.NyeBrukereFeedRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class AktiverBrukerServiceTest {

    private AuthService authService;
    private AktiverBrukerService aktiverBrukerService;
    private BehandleArbeidssokerClient behandleArbeidssokerClient;
    private OppfolgingRepositoryService oppfolgingRepositoryService;
    private NyeBrukereFeedRepository nyeBrukereFeedRepository;

    @BeforeEach
    public void setup() {
        authService = mock(AuthService.class);
        behandleArbeidssokerClient = mock(BehandleArbeidssokerClient.class);
        oppfolgingRepositoryService = mock(OppfolgingRepositoryService.class);
        nyeBrukereFeedRepository = mock(NyeBrukereFeedRepository.class);

        aktiverBrukerService =
                new AktiverBrukerService(
                        authService,
                        oppfolgingRepositoryService,
                        behandleArbeidssokerClient,
                        nyeBrukereFeedRepository);

//        when(aktorService.getAktorId(any())).thenReturn(Optional.of("AKTORID"));
    }

    private AktiverArbeidssokerData hentBruker() {
        return new AktiverArbeidssokerData(new Fnr("fnr"), Innsatsgruppe.STANDARD_INNSATS);
    }

    @Test
    void skalRegistrereIArena() {
        aktiverBrukerService.aktiverBruker(hentBruker());
//        verify(behandleArbeidssoekerV1, times(1)).aktiverBruker(any());
    }

    @Test
    void brukerSomHarInaktivStatusSkalKunneReaktivereSeg() {
        aktiverBrukerService.reaktiverBruker(new Fnr("fnr"));
//        verify(behandleArbeidssoekerV1, times(1)).reaktiverBrukerForenklet(any());
    }

    @Test
    void brukerSomIkkeKanReaktiveresForenkletIArenaSkalGiRiktigFeil() {
        doThrow(mock(ReaktiverBrukerForenkletBrukerKanIkkeReaktiveresForenklet.class)).when(aktiverBrukerService).reaktiverBruker(any());
//        Feil e = reaktiverBrukerMotArenaOgReturnerFeil(new Fnr("fnr"));
//        assertThat(e.getType().getStatus()).isNotNull();
//        assertThat(e.getType().getStatus()).isEqualTo(FORBIDDEN);
//        assertThat(e.getType().getName()).isEqualTo("BRUKER_KAN_IKKE_REAKTIVERES_FORENKLET");
    }

    @Test
    void brukerSomIkkeFinnesIArenaSkalGiRiktigStatus() {
        doThrow(mock(AktiverBrukerBrukerFinnesIkke.class)).when(aktiverBrukerService).aktiverBruker(any());
//        Feil e = aktiverBrukerMotArenaOgReturnerFeil(hentBruker());
//        assertThat(e.getType().getStatus()).isNotNull();
//        assertThat(e.getType().getStatus()).isEqualTo(FORBIDDEN);
//        assertThat(e.getType().getName()).isEqualTo("BRUKER_ER_UKJENT");
    }

    @Test
    void brukerSomIkkeKanReaktiveresIArenaSkalGiGiRiktigStatus() {
        doThrow(mock(AktiverBrukerBrukerIkkeReaktivert.class)).when(aktiverBrukerService).aktiverBruker(any());
//        Feil e = aktiverBrukerMotArenaOgReturnerFeil(hentBruker());
//        assertThat(e.getType().getStatus()).isNotNull();
//        assertThat(e.getType().getStatus()).isEqualTo(FORBIDDEN);
//        assertThat(e.getType().getName()).isEqualTo("BRUKER_KAN_IKKE_REAKTIVERES");
    }

    @Test
    void brukerSomIkkeKanAktiveresIArenaSkalGiRiktigStatus() {
        doThrow(mock(AktiverBrukerBrukerKanIkkeAktiveres.class)).when(aktiverBrukerService).aktiverBruker(any());
//        Feil e = aktiverBrukerMotArenaOgReturnerFeil(hentBruker());
//        assertThat(e.getType().getStatus()).isNotNull();
//        assertThat(e.getType().getStatus()).isEqualTo(FORBIDDEN);
//        assertThat(e.getType().getName()).isEqualTo("BRUKER_ER_DOD_UTVANDRET_ELLER_FORSVUNNET");
    }

    @Test
    void brukerSomManglerArbeidstillatelseSkalGiRiktigStatus() {
        doThrow(mock(AktiverBrukerBrukerManglerArbeidstillatelse.class)).when(aktiverBrukerService).aktiverBruker(any());
//        Feil e = aktiverBrukerMotArenaOgReturnerFeil(hentBruker());
//        assertThat(e.getType().getStatus()).isNotNull();
//        assertThat(e.getType().getStatus()).isEqualTo(FORBIDDEN);
//        assertThat(e.getType().getName()).isEqualTo("BRUKER_MANGLER_ARBEIDSTILLATELSE");
    }

    @Test
    void brukerSomIkkeHarTilgangSkalGiNotAuthorizedException() {
        doThrow(mock(AktiverBrukerSikkerhetsbegrensning.class)).when(aktiverBrukerService).aktiverBruker(any());
        assertThrows(NotAuthorizedException.class, () -> aktiverBrukerService.aktiverBruker(hentBruker()));
    }

    @Test
    void ugyldigInputSkalGiBadRequestException() {
        doThrow(mock(AktiverBrukerUgyldigInput.class)).when(aktiverBrukerService).aktiverBruker(any());
        assertThrows(BadRequestException.class, () -> aktiverBrukerService.aktiverBruker(hentBruker()));
    }

}