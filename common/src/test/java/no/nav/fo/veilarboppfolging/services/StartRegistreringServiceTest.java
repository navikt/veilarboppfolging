package no.nav.fo.veilarboppfolging.services;

import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.ArbeidssokerregistreringRepository;
import no.nav.fo.veilarboppfolging.domain.StartRegistreringStatus;
import no.nav.tjeneste.virksomhet.behandleoppfolging.v1.binding.HentStartRegistreringStatusFeilVedHentingAvStatusFraArena;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.HentOppfoelgingsstatusPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

public class StartRegistreringServiceTest {
    private OppfoelgingPortType oppfoelgingPortType;
    private PepClient pepClient;
    private AktorService aktorService;
    private ArbeidssokerregistreringRepository arbeidssokerregistreringRepository;
    private StartRegistreringService startRegistreringService;

    @BeforeEach
    public void setup() {
        oppfoelgingPortType = mock(OppfoelgingPortType.class);
        pepClient = mock(PepClient.class);
        aktorService = mock(AktorService.class);
        arbeidssokerregistreringRepository = mock(ArbeidssokerregistreringRepository.class);
        startRegistreringService = new StartRegistreringService(
                arbeidssokerregistreringRepository,
                pepClient,
                aktorService,
                oppfoelgingPortType
        );
    }


    @Test
    public void skalIkkeKalleArenaOmOppfolgingsflaggErSatt() throws Exception {
        when(aktorService.getAktorId(any())).thenReturn(Optional.of("1111"));
        when(arbeidssokerregistreringRepository.erOppfolgingsflaggSatt(any())).thenReturn(true);
        StartRegistreringStatus status = startRegistreringService.hentStartRegistreringStatus("1111111111");
        assertThat(status.isUnderOppfolging()).isTrue();
        verify(oppfoelgingPortType, never()).hentOppfoelgingsstatus(any());
    }

    @Test
    public void abacSkalKallesForAlleRequests() throws Exception {
        when(aktorService.getAktorId(any())).thenReturn(Optional.of("1111"));
        when(arbeidssokerregistreringRepository.erOppfolgingsflaggSatt(any())).thenReturn(true);
        StartRegistreringStatus status = startRegistreringService.hentStartRegistreringStatus("1111111111");
        verify(pepClient, times(1)).sjekkLeseTilgangTilFnr("1111111111");
    }

    @Test
    public void skalOppfylleKravOmBrukerIkkeFinnesIArenaOgOppfyllerKravOmAlder() throws Exception {
        when(aktorService.getAktorId(any())).thenReturn(Optional.of("1111"));
        when(arbeidssokerregistreringRepository.erOppfolgingsflaggSatt(any())).thenReturn(false);
        when(oppfoelgingPortType.hentOppfoelgingsstatus(any())).thenThrow(HentOppfoelgingsstatusPersonIkkeFunnet.class);
        StartRegistreringStatus status = startRegistreringService.hentStartRegistreringStatus("***REMOVED***");
        assertThat(status.isUnderOppfolging()).isFalse();
        assertThat(status.isOppfyllerKravForAutomatiskRegistrering()).isTrue();
    }

    @Test
    public void skalKasteExceptionVedFeilendeKallTilArena() throws Exception {
        when(aktorService.getAktorId(any())).thenReturn(Optional.of("1111"));
        when(arbeidssokerregistreringRepository.erOppfolgingsflaggSatt(any())).thenReturn(false);
        when(oppfoelgingPortType.hentOppfoelgingsstatus(any())).thenThrow(Exception.class);
        assertThrows(HentStartRegistreringStatusFeilVedHentingAvStatusFraArena.class, () -> startRegistreringService.hentStartRegistreringStatus("***REMOVED***"));
    }


}