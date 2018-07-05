package no.nav.fo.veilarboppfolging.services;

import no.nav.apiapp.feil.Feil;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.config.RemoteFeatureConfig;
import no.nav.fo.veilarboppfolging.db.NyeBrukereFeedRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.domain.AktiverArbeidssokerData;
import no.nav.fo.veilarboppfolging.domain.Fnr;
import no.nav.fo.veilarboppfolging.domain.Innsatsgruppe;
import no.nav.fo.veilarboppfolging.domain.Oppfolgingsbruker;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.binding.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
import java.util.Optional;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AktiverBrukerServiceTest {

    private AktorService aktorService;
    private AktiverBrukerService aktiverBrukerService;
    private BehandleArbeidssoekerV1 behandleArbeidssoekerV1;
    private RemoteFeatureConfig.OpprettBrukerIArenaFeature opprettBrukerIArenaFeature;
    private RemoteFeatureConfig.RegistreringFeature registreringFeature;
    private OppfolgingRepository oppfolgingRepository;
    private NyeBrukereFeedRepository nyeBrukereFeedRepository;
    private PepClient pepClient;

    @BeforeEach
    public void setup() {
        opprettBrukerIArenaFeature = mock(RemoteFeatureConfig.OpprettBrukerIArenaFeature.class);
        registreringFeature = mock(RemoteFeatureConfig.RegistreringFeature.class);
        aktorService = mock(AktorService.class);
        behandleArbeidssoekerV1 = mock(BehandleArbeidssoekerV1.class);
        oppfolgingRepository = mock(OppfolgingRepository.class);
        nyeBrukereFeedRepository = mock(NyeBrukereFeedRepository.class);
        pepClient = mock(PepClient.class);

        aktiverBrukerService =
                new AktiverBrukerService(
                        oppfolgingRepository,
                        aktorService,
                        behandleArbeidssoekerV1,
                        opprettBrukerIArenaFeature,
                        registreringFeature,
                        nyeBrukereFeedRepository,
                        pepClient);

        when(aktorService.getAktorId(any())).thenReturn(Optional.of("AKTORID"));
        when(opprettBrukerIArenaFeature.erAktiv()).thenReturn(true);
        when(registreringFeature.erAktiv()).thenReturn(true);
    }

    @Test
    void skalAktivereOppfolgingPaaBrukerIDatabasenSelvOmArenaErToggletBort() throws Exception {
        when(opprettBrukerIArenaFeature.erAktiv()).thenReturn(false);
        aktiverBrukerService.aktiverBruker(hentBruker());
        verify(behandleArbeidssoekerV1, times(0)).aktiverBruker(any());
        verify(oppfolgingRepository, times(1)).startOppfolgingHvisIkkeAlleredeStartet(any(Oppfolgingsbruker.class));
    }


    private AktiverArbeidssokerData hentBruker() {
        return new AktiverArbeidssokerData(new Fnr("12345678910"), Innsatsgruppe.STANDARD_INNSATS);
    }

    @Test
    void skalRegistrereIArenaNaarArenaToggleErPaa() throws Exception {
        aktiverBrukerService.aktiverBruker(hentBruker());
        when(opprettBrukerIArenaFeature.erAktiv()).thenReturn(true);
        verify(behandleArbeidssoekerV1, times(1)).aktiverBruker(any());
    }

    @Test
    void skalKasteRuntimeExceptionDersomRegistreringFeatureErAv() throws Exception {
        when(registreringFeature.erAktiv()).thenReturn(false);
        assertThrows(RuntimeException.class, () -> aktiverBrukerService.aktiverBruker(hentBruker()));
        verify(behandleArbeidssoekerV1, times(0)).aktiverBruker(any());
    }

    @Test
    void brukerSomIkkeFinnesIArenaSkalGiRiktigStatus() throws Exception {
        when(registreringFeature.erAktiv()).thenReturn(true);
        doThrow(mock(AktiverBrukerBrukerFinnesIkke.class)).when(behandleArbeidssoekerV1).aktiverBruker(any());
        Feil e = aktiverBrukerMotArenaOgReturnerFeil(hentBruker());
        assertThat(e.getType().getStatus()).isNotNull();
        assertThat(e.getType().getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(e.getType().getName()).isEqualTo("BRUKER_ER_UKJENT");
    }

    @Test
    void brukerSomIkkeKanReaktiveresIArenaSkalGiGiRiktigStatus() throws Exception {
        doThrow(mock(AktiverBrukerBrukerIkkeReaktivert.class)).when(behandleArbeidssoekerV1).aktiverBruker(any());
        Feil e = aktiverBrukerMotArenaOgReturnerFeil(hentBruker());
        assertThat(e.getType().getStatus()).isNotNull();
        assertThat(e.getType().getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(e.getType().getName()).isEqualTo("BRUKER_KAN_IKKE_REAKTIVERES");
    }

    @Test
    void brukerSomIkkeKanAktiveresIArenaSkalGiRiktigStatus() throws Exception {
        doThrow(mock(AktiverBrukerBrukerKanIkkeAktiveres.class)).when(behandleArbeidssoekerV1).aktiverBruker(any());
        Feil e = aktiverBrukerMotArenaOgReturnerFeil(hentBruker());
        assertThat(e.getType().getStatus()).isNotNull();
        assertThat(e.getType().getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(e.getType().getName()).isEqualTo("BRUKER_ER_DOD_UTVANDRET_ELLER_FORSVUNNET");
    }

    @Test
    void brukerSomManglerArbeidstillatelseSkalGiRiktigStatus() throws Exception {
        doThrow(mock(AktiverBrukerBrukerManglerArbeidstillatelse.class)).when(behandleArbeidssoekerV1).aktiverBruker(any());
        Feil e = aktiverBrukerMotArenaOgReturnerFeil(hentBruker());
        assertThat(e.getType().getStatus()).isNotNull();
        assertThat(e.getType().getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThat(e.getType().getName()).isEqualTo("BRUKER_MANGLER_ARBEIDSTILLATELSE");
    }

    @Test
    void brukerSomIkkeHarTilgangSkalGiNotAuthorizedException() throws Exception {
        doThrow(mock(AktiverBrukerSikkerhetsbegrensning.class)).when(behandleArbeidssoekerV1).aktiverBruker(any());
        assertThrows(NotAuthorizedException.class, () -> aktiverBrukerService.aktiverBruker(hentBruker()));
    }

    @Test
    void ugyldigInputSkalGiBadRequestException() throws Exception {
        doThrow(mock(AktiverBrukerUgyldigInput.class)).when(behandleArbeidssoekerV1).aktiverBruker(any());
        assertThrows(BadRequestException.class, () -> aktiverBrukerService.aktiverBruker(hentBruker()));
    }

    private Feil aktiverBrukerMotArenaOgReturnerFeil(AktiverArbeidssokerData aktiverArbeidssokerData) {
        try {
            aktiverBrukerService.aktiverBruker(aktiverArbeidssokerData);
        } catch (Feil feil) {
            return feil;
        }
        return null;
    }

}