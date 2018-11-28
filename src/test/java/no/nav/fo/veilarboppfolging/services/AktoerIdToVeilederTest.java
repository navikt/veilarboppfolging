package no.nav.fo.veilarboppfolging.services;


import no.nav.apiapp.security.PepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.feed.producer.FeedProducer;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.db.VeilederTilordningerRepository;
import no.nav.fo.veilarboppfolging.rest.AutorisasjonService;
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

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class AktoerIdToVeilederTest {

    @Mock
    private VeilederTilordningerRepository veilederTilordningerRepository;

    @Mock
    private OppfolgingRepository oppfolgingRepository;

    @Mock
    private AktorService aktorServiceMock;

    @Mock
    private FeedProducer<OppfolgingFeedDTO> feed;

    @Mock
    private AutorisasjonService autorisasjonService;

    @InjectMocks
    private VeilederTilordningRessurs veilederTilordningRessurs;

    @Mock
    private PepClient pepClient;

    @Test
    public void portefoljeRessursMustCallDAOwithAktoerIdToVeileder() throws PepException {
        when(aktorServiceMock.getAktorId(any(String.class))).thenReturn(of("AKTOERID"));
        veilederTilordningRessurs.postVeilederTilordninger(Collections.singletonList(testData()));
        verify(veilederTilordningerRepository, times(1)).upsertVeilederTilordning(anyString(), anyString());
    }

    public void noCallToDAOWhenAktoerIdServiceFails() {
        when(aktorServiceMock.getAktorId(any(String.class))).thenReturn(empty());
        veilederTilordningRessurs.postVeilederTilordninger(Collections.singletonList(testData()));
        verify(veilederTilordningerRepository, never()).upsertVeilederTilordning(anyString(), anyString());
    }

    private VeilederTilordning testData() {
        return new VeilederTilordning()
                .setFraVeilederId(null)
                .setTilVeilederId("4321")
                .setBrukerFnr("1234");
    }
}
