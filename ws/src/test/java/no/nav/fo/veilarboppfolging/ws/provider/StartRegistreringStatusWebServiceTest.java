package no.nav.fo.veilarboppfolging.ws.provider;

import cxf.FeilVedHentingAvStatusFraArenaException;
import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.ArbeidssokerregistreringRepository;
import no.nav.fo.veilarboppfolging.ws.provider.startregistrering.StartRegistreringStatusWebService;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.HentOppfoelgingsstatusPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.startregistreringstatus.v1.meldinger.WSStartRegistreringStatus;
import no.nav.tjeneste.virksomhet.startregistreringstatus.v1.meldinger.WSStartRegistreringStatusRequest;
import no.nav.tjeneste.virksomhet.startregistreringstatus.v1.meldinger.WSStartRegistreringStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class StartRegistreringStatusWebServiceTest {

    private OppfoelgingPortType oppfoelgingPortType;
    private PepClient pepClient;
    private AktorService aktorService;
    private ArbeidssokerregistreringRepository arbeidssokerregistreringRepository;
    private StartRegistreringStatusWebService startRegistreringStatusWebService;

    @BeforeEach
    public void setup() {
        oppfoelgingPortType = mock(OppfoelgingPortType.class);
        pepClient = mock(PepClient.class);
        aktorService = mock(AktorService.class);
        arbeidssokerregistreringRepository = mock(ArbeidssokerregistreringRepository.class);
        startRegistreringStatusWebService = new StartRegistreringStatusWebService(oppfoelgingPortType,
                pepClient,
                aktorService,
                arbeidssokerregistreringRepository);
    }


    @Test
    public void skalIkkeKalleArenaOmOppfolgingsflaggErSatt() throws Exception {
        when(aktorService.getAktorId(any())).thenReturn(Optional.of("1111"));
        when(arbeidssokerregistreringRepository.erOppfolgingsflaggSatt(any())).thenReturn(true);
        WSStartRegistreringStatusRequest request = new WSStartRegistreringStatusRequest();
        request.setFnr("1111111111");
        WSStartRegistreringStatus status = startRegistreringStatusWebService.hentStartRegistreringStatus(request).getWSStartRegistreringStatus();
        assertThat(status.isErUnderOppfolging()).isTrue();
        verify(oppfoelgingPortType, never()).hentOppfoelgingsstatus(any());
    }

    @Test
    public void abacSkalKallesForAlleRequests() throws Exception {
        when(aktorService.getAktorId(any())).thenReturn(Optional.of("1111"));
        when(arbeidssokerregistreringRepository.erOppfolgingsflaggSatt(any())).thenReturn(true);
        WSStartRegistreringStatusRequest request = new WSStartRegistreringStatusRequest();
        request.setFnr("1111111111");
        WSStartRegistreringStatus status = startRegistreringStatusWebService.hentStartRegistreringStatus(request).getWSStartRegistreringStatus();
        verify(pepClient, times(1)).sjekkLeseTilgangTilFnr("1111111111");
    }

    @Test
    public void skalOppfylleKravOmBrukerIkkeFinnesIArenaOgOppfyllerKravOmAlder() throws Exception {
        when(aktorService.getAktorId(any())).thenReturn(Optional.of("1111"));
        when(arbeidssokerregistreringRepository.erOppfolgingsflaggSatt(any())).thenReturn(false);
        when(oppfoelgingPortType.hentOppfoelgingsstatus(any())).thenThrow(HentOppfoelgingsstatusPersonIkkeFunnet.class);
        WSStartRegistreringStatusRequest request = new WSStartRegistreringStatusRequest();
        request.setFnr("***REMOVED***");
        WSStartRegistreringStatusResponse response = startRegistreringStatusWebService.hentStartRegistreringStatus(request);
        assertThat(response.getWSStartRegistreringStatus().isErUnderOppfolging()).isFalse();
        assertThat(response.getWSStartRegistreringStatus().isOppfyllerKrav()).isTrue();
    }

    @Test
    public void skalKasteExceptionVedFeilendeKallTilArena() throws Exception {
        when(aktorService.getAktorId(any())).thenReturn(Optional.of("1111"));
        when(arbeidssokerregistreringRepository.erOppfolgingsflaggSatt(any())).thenReturn(false);
        when(oppfoelgingPortType.hentOppfoelgingsstatus(any())).thenThrow(Exception.class);
        WSStartRegistreringStatusRequest request = new WSStartRegistreringStatusRequest();
        request.setFnr("***REMOVED***");
        assertThrows(FeilVedHentingAvStatusFraArenaException.class, () -> startRegistreringStatusWebService.hentStartRegistreringStatus(request));
    }
}