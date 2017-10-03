package no.nav.fo.veilarboppfolging.services;


import no.nav.apiapp.security.PepClient;
import no.nav.fo.feed.producer.FeedProducer;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.db.VeilederTilordningerRepository;
import no.nav.fo.veilarboppfolging.rest.VeilederTilordningRessurs;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingFeedDTO;
import no.nav.fo.veilarboppfolging.rest.domain.VeilederTilordning;
import no.nav.sbl.dialogarena.common.abac.pep.exception.PepException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class AktoerIdToVeilederTest {

    @Mock
    private VeilederTilordningerRepository veilederTilordningerRepository;

    @Mock
    private OppfolgingRepository oppfolgingRepository;

    @Mock
    private AktoerIdService aktoerIdService;

    @Mock
    private FeedProducer<OppfolgingFeedDTO> feed;

    @InjectMocks
    private VeilederTilordningRessurs veilederTilordningRessurs;

    @Mock
    private PepClient pepClient;

    @Test
    public void portefoljeRessursMustCallDAOwithAktoerIdToVeileder() throws PepException {
        when(aktoerIdService.findAktoerId(any(String.class))).thenReturn("AKTOERID");
        veilederTilordningRessurs.postVeilederTilordninger(Collections.singletonList(testData()));
        verify(veilederTilordningerRepository, times(1)).upsertVeilederTilordning(anyString(), anyString());
    }

    public void noCallToDAOWhenAktoerIdServiceFails() {
        when(aktoerIdService.findAktoerId(any(String.class))).thenReturn(null);
        veilederTilordningRessurs.postVeilederTilordninger(Collections.singletonList(testData()));
        verify(veilederTilordningerRepository, never()).upsertVeilederTilordning(anyString(), anyString());
    }

    private VeilederTilordning testData() {
        return new VeilederTilordning()
                .setFraVeilederId(null)
                .setTilVeilederId("***REMOVED***")
                .setBrukerFnr("12345678912");
    }
}