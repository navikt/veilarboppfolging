package no.nav.veilarboppfolging.controller;

import no.nav.dialogarena.aktor.AktorService;
import no.nav.veilarboppfolging.db.VeilederTilordningerRepository;
import no.nav.veilarboppfolging.controller.domain.Veileder;
import no.nav.veilarboppfolging.services.AutorisasjonService;
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

    @Mock
    private AutorisasjonService autorisasjonService;

    @Test
    public void getVeilederSkalReturnereVeileder() {
        final String forventetIdent = "4321";
        when(aktorServiceMock.getAktorId(anyString())).thenReturn(of("test-id"));
        when(veilederTilordningerRepository.hentTilordningForAktoer(anyString()))
                .thenReturn(forventetIdent);

        final Veileder veileder = veilederRessurs.getVeileder("fnr");
        assertNotNull(veileder);
        assertNotNull(veileder.getVeilederident());
        assertThat(veileder.getVeilederident(), is(forventetIdent));
    }
}
