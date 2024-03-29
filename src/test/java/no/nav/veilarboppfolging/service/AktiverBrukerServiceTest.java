package no.nav.veilarboppfolging.service;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.behandle_arbeidssoker.BehandleArbeidssokerClient;
import no.nav.veilarboppfolging.controller.request.Innsatsgruppe;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AktiverBrukerServiceTest {

    private final static Fnr FNR = Fnr.of("12343312");

    private final static AktorId AKTOR_ID = AktorId.of("1234767554");

    private AuthService authService;

    private BehandleArbeidssokerClient behandleArbeidssokerClient;

    private OppfolgingService oppfolgingService;

    private AktiverBrukerService aktiverBrukerService;

    @Before
    public void setup() {
        authService = mock(AuthService.class);
        behandleArbeidssokerClient = mock(BehandleArbeidssokerClient.class);
        oppfolgingService = mock(OppfolgingService.class);

        aktiverBrukerService = new AktiverBrukerService(
                authService,
                oppfolgingService,
                behandleArbeidssokerClient,
                DbTestUtils.createTransactor(LocalH2Database.getDb())
        );
    }

    @Test
    public void skalRegistrereIArena() {
        when(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID);
        aktiverBrukerService.aktiverBruker(FNR, Innsatsgruppe.STANDARD_INNSATS);
        verify(behandleArbeidssokerClient, times(1)).opprettBrukerIArena(any(), any());
    }

    @Test
    public void brukerSomHarInaktivStatusSkalKunneReaktivereSeg() {
        when(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID);
        aktiverBrukerService.reaktiverBruker(FNR);
        verify(behandleArbeidssokerClient, times(1)).reaktiverBrukerIArena(any());
    }

    // TODO: Trenger vi spisset feil eller holder det med en generisk feil
    @Ignore
    @Test
    public void brukerSomIkkeKanReaktiveresForenkletIArenaSkalGiRiktigFeil() {
//        doThrow(mock(ReaktiverBrukerForenkletBrukerKanIkkeReaktiveresForenklet.class)).when(aktiverBrukerService).reaktiverBruker(any());
//        Feil e = reaktiverBrukerMotArenaOgReturnerFeil(new Fnr("fnr"));
//        assertThat(e.getType().getStatus()).isNotNull();
//        assertThat(e.getType().getStatus()).isEqualTo(FORBIDDEN);
//        assertThat(e.getType().getName()).isEqualTo("BRUKER_KAN_IKKE_REAKTIVERES_FORENKLET");
    }

    // TODO: Trenger vi spisset feil eller holder det med en generisk feil
    @Ignore
    @Test
    public void brukerSomIkkeFinnesIArenaSkalGiRiktigStatus() {
//        doThrow(mock(AktiverBrukerBrukerFinnesIkke.class)).when(aktiverBrukerService).aktiverBruker(any());
//        Feil e = aktiverBrukerMotArenaOgReturnerFeil(hentBruker());
//        assertThat(e.getType().getStatus()).isNotNull();
//        assertThat(e.getType().getStatus()).isEqualTo(FORBIDDEN);
//        assertThat(e.getType().getName()).isEqualTo("BRUKER_ER_UKJENT");
    }

    // TODO: Trenger vi spisset feil eller holder det med en generisk feil
    @Ignore
    @Test
    public void brukerSomIkkeKanReaktiveresIArenaSkalGiGiRiktigStatus() {
//        doThrow(mock(AktiverBrukerBrukerIkkeReaktivert.class)).when(aktiverBrukerService).aktiverBruker(any());
//        Feil e = aktiverBrukerMotArenaOgReturnerFeil(hentBruker());
//        assertThat(e.getType().getStatus()).isNotNull();
//        assertThat(e.getType().getStatus()).isEqualTo(FORBIDDEN);
//        assertThat(e.getType().getName()).isEqualTo("BRUKER_KAN_IKKE_REAKTIVERES");
    }

    // TODO: Trenger vi spisset feil eller holder det med en generisk feil
    @Ignore
    @Test
    public void brukerSomIkkeKanAktiveresIArenaSkalGiRiktigStatus() {
//        doThrow(mock(AktiverBrukerBrukerKanIkkeAktiveres.class)).when(aktiverBrukerService).aktiverBruker(any());
//        Feil e = aktiverBrukerMotArenaOgReturnerFeil(hentBruker());
//        assertThat(e.getType().getStatus()).isNotNull();
//        assertThat(e.getType().getStatus()).isEqualTo(FORBIDDEN);
//        assertThat(e.getType().getName()).isEqualTo("BRUKER_ER_DOD_UTVANDRET_ELLER_FORSVUNNET");
    }

    // TODO: Trenger vi spisset feil eller holder det med en generisk feil
    @Ignore
    @Test
    public void brukerSomManglerArbeidstillatelseSkalGiRiktigStatus() {
//        doThrow(mock(AktiverBrukerBrukerManglerArbeidstillatelse.class)).when(aktiverBrukerService).aktiverBruker(any());
//        Feil e = aktiverBrukerMotArenaOgReturnerFeil(hentBruker());
//        assertThat(e.getType().getStatus()).isNotNull();
//        assertThat(e.getType().getStatus()).isEqualTo(FORBIDDEN);
//        assertThat(e.getType().getName()).isEqualTo("BRUKER_MANGLER_ARBEIDSTILLATELSE");
    }

    // TODO: Trenger vi spisset feil eller holder det med en generisk feil
    @Ignore
    @Test
    public void brukerSomIkkeHarTilgangSkalGiNotAuthorizedException() {
//        doThrow(mock(AktiverBrukerSikkerhetsbegrensning.class)).when(aktiverBrukerService).aktiverBruker(any());
//        assertThrows(NotAuthorizedException.class, () -> aktiverBrukerService.aktiverBruker(hentBruker()));
    }

    // TODO: Trenger vi spisset feil eller holder det med en generisk feil
    @Ignore
    @Test
    public void ugyldigInputSkalGiBadRequestException() {
//        doThrow(mock(AktiverBrukerUgyldigInput.class)).when(aktiverBrukerService).aktiverBruker(any());
//        assertThrows(BadRequestException.class, () -> aktiverBrukerService.aktiverBruker(hentBruker()));
    }

}
