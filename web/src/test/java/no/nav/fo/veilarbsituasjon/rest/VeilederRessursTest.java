package no.nav.fo.veilarbsituasjon.rest;

import no.nav.fo.veilarbsituasjon.db.BrukerRepository;
import no.nav.fo.veilarbsituasjon.rest.domain.OppfolgingBruker;
import no.nav.fo.veilarbsituasjon.rest.domain.Veileder;
import no.nav.fo.veilarbsituasjon.services.AktoerIdService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VeilederRessursTest {

    @InjectMocks
    private VeilederRessurs veilederRessurs;

    @Mock
    private BrukerRepository brukerRepository;

    @Mock
    private AktoerIdService aktoerIdService;

    @Test
    public void getVeilederSkalReturnereVeileder() throws Exception {
        final String forventetIdent = "***REMOVED***";
        when(aktoerIdService.findAktoerId(anyString())).thenReturn("test-id");
        when(brukerRepository.hentTilordningForAktoer(anyString()))
                .thenReturn(OppfolgingBruker.builder().veileder(forventetIdent).build());

        final Veileder veileder = veilederRessurs.getVeileder("***REMOVED***");
        assertNotNull(veileder);
        assertNotNull(veileder.getVeilederident());
        assertThat(veileder.getVeilederident(), is(forventetIdent));
    }
}
