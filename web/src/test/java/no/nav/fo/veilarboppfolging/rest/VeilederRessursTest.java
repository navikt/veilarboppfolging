package no.nav.fo.veilarboppfolging.rest;

import no.nav.fo.veilarboppfolging.db.VeilederTilordningerRepository;
import no.nav.fo.veilarboppfolging.rest.domain.Veileder;
import no.nav.dialogarena.aktor.AktorService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.util.Optional.of;
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
    private VeilederTilordningerRepository veilederTilordningerRepository;

    @Mock
    private AktorService aktorServiceMock;

    @Test
    public void getVeilederSkalReturnereVeileder() throws Exception {
        final String forventetIdent = "***REMOVED***";
        when(aktorServiceMock.getAktorId(anyString())).thenReturn(of("test-id"));
        when(veilederTilordningerRepository.hentTilordningForAktoer(anyString()))
                .thenReturn(forventetIdent);

        final Veileder veileder = veilederRessurs.getVeileder("***REMOVED***");
        assertNotNull(veileder);
        assertNotNull(veileder.getVeilederident());
        assertThat(veileder.getVeilederident(), is(forventetIdent));
    }
}
