package no.nav.fo.veilarboppfolging.services;


import no.nav.apiapp.security.PepClient;
import no.nav.fo.feed.producer.FeedProducer;
import no.nav.fo.veilarboppfolging.db.BrukerRepository;
import no.nav.fo.veilarboppfolging.db.SituasjonRepository;
import no.nav.fo.veilarboppfolging.rest.PortefoljeRessurs;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingBruker;
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
    private BrukerRepository brukerRepository;

    @Mock
    private SituasjonRepository situasjonRepository;

    @Mock
    private AktoerIdService aktoerIdService;

    @Mock
    private FeedProducer<OppfolgingBruker> feed;

    @InjectMocks
    private PortefoljeRessurs portefoljeRessurs;

    @Mock
    private PepClient pepClient;

    @Test
    public void portefoljeRessursMustCallDAOwithAktoerIdToVeileder() throws PepException {
        when(aktoerIdService.findAktoerId(any(String.class))).thenReturn("AKTOERID");
        portefoljeRessurs.postVeilederTilordninger(Collections.singletonList(testData()));
        verify(brukerRepository, times(1)).upsertVeilederTilordning(anyString(), anyString());
    }

    public void noCallToDAOWhenAktoerIdServiceFails() {
        when(aktoerIdService.findAktoerId(any(String.class))).thenReturn(null);
        portefoljeRessurs.postVeilederTilordninger(Collections.singletonList(testData()));
        verify(brukerRepository, never()).upsertVeilederTilordning(anyString(), anyString());
    }

    private VeilederTilordning testData() {
        return new VeilederTilordning()
                .setFraVeilederId(null)
                .setTilVeilederId("***REMOVED***")
                .setBrukerFnr("12345678912");
    }
}