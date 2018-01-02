package no.nav.fo.veilarboppfolging.services;

import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.ArbeidssokerregistreringRepository;
import no.nav.fo.veilarboppfolging.domain.StartRegistreringStatus;
import no.nav.fo.veilarboppfolging.services.startregistrering.StartRegistreringService;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentStartRegistreringStatusFeilVedHentingAvStatusFraArena;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.NotFoundException;
import java.util.Optional;

import static no.nav.fo.veilarboppfolging.utils.StartRegistreringUtilsTest.getArbeidsforholdSomOppfyllerKrav;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

public class StartRegistreringServiceTest {
    private ArenaOppfolgingService arenaOppfolgingService;
    private PepClient pepClient;
    private AktorService aktorService;
    private ArbeidssokerregistreringRepository arbeidssokerregistreringRepository;
    private StartRegistreringService startRegistreringService;
    private ArbeidsforholdService arbeidsforholdService;

    @BeforeEach
    public void setup() {
        arenaOppfolgingService = mock(ArenaOppfolgingService.class);
        pepClient = mock(PepClient.class);
        aktorService = mock(AktorService.class);
        arbeidssokerregistreringRepository = mock(ArbeidssokerregistreringRepository.class);
        arbeidsforholdService = mock(ArbeidsforholdService.class);
        startRegistreringService = new StartRegistreringService(
                arbeidssokerregistreringRepository,
                pepClient,
                aktorService,
                arenaOppfolgingService,
                arbeidsforholdService
        );
    }


    @Test
    public void skalIkkeKalleArenaOmOppfolgingsflaggErSatt() throws Exception {
        when(aktorService.getAktorId(any())).thenReturn(Optional.of("1111"));
        when(arbeidssokerregistreringRepository.erOppfolgingsflaggSatt(any())).thenReturn(true);
        StartRegistreringStatus status = startRegistreringService.hentStartRegistreringStatus("1111111111");
        assertThat(status.isUnderOppfolging()).isTrue();
        verify(arenaOppfolgingService, never()).hentArenaOppfolging(any());
    }

    @Test
    public void abacSkalKallesForAlleRequests() throws Exception {
        when(aktorService.getAktorId(any())).thenReturn(Optional.of("1111"));
        when(arbeidssokerregistreringRepository.erOppfolgingsflaggSatt(any())).thenReturn(true);
        StartRegistreringStatus status = startRegistreringService.hentStartRegistreringStatus("1111111111");
        verify(pepClient, times(1)).sjekkLeseTilgangTilFnr("1111111111");
    }

    @Test
    public void skalOppfylleKravOmBrukerIkkeFinnesIArenaOgOppfyllerKravOmAlderogArbeidserfaring() throws Exception {
        when(aktorService.getAktorId(any())).thenReturn(Optional.of("1111"));
        when(arbeidssokerregistreringRepository.erOppfolgingsflaggSatt(any())).thenReturn(false);
        when(arenaOppfolgingService.hentArenaOppfolging(any())).thenThrow(NotFoundException.class);
        when(arbeidsforholdService.hentArbeidsforhold(any())).thenReturn(getArbeidsforholdSomOppfyllerKrav());
        StartRegistreringStatus status = startRegistreringService.hentStartRegistreringStatus("***REMOVED***");
        assertThat(status.isUnderOppfolging()).isFalse();
        assertThat(status.isOppfyllerKravForAutomatiskRegistrering()).isTrue();
    }

    @Test
    public void skalKasteExceptionVedFeilendeKallTilArena() throws Exception {
        when(aktorService.getAktorId(any())).thenReturn(Optional.of("1111"));
        when(arbeidssokerregistreringRepository.erOppfolgingsflaggSatt(any())).thenReturn(false);
        when(arenaOppfolgingService.hentArenaOppfolging(any())).thenThrow(Exception.class);
        assertThrows(HentStartRegistreringStatusFeilVedHentingAvStatusFraArena.class, () -> startRegistreringService.hentStartRegistreringStatus("***REMOVED***"));
    }
}