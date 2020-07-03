package no.nav.veilarboppfolging.controller;

import no.nav.veilarboppfolging.controller.domain.Veileder;
import no.nav.veilarboppfolging.repository.VeilederTilordningerRepository;
import no.nav.veilarboppfolging.services.AuthService;
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
public class VeilederControllerTest {

    @InjectMocks
    private VeilederController veilederController;

    @Mock
    private VeilederTilordningerRepository veilederTilordningerRepository;

    @Mock
    private AuthService authService;

    @Test
    public void getVeilederSkalReturnereVeileder() {
        final String forventetIdent = "4321";
        when(authService.getAktorIdOrThrow(anyString())).thenReturn("test-id");
        when(veilederTilordningerRepository.hentTilordningForAktoer(anyString()))
                .thenReturn(forventetIdent);

        final Veileder veileder = veilederController.getVeileder("fnr");
        assertNotNull(veileder);
        assertNotNull(veileder.getVeilederident());
        assertThat(veileder.getVeilederident(), is(forventetIdent));
    }
}
