package no.nav.veilarboppfolging.controller;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.Veileder;
import no.nav.veilarboppfolging.repository.VeilederTilordningerRepository;
import no.nav.veilarboppfolging.service.AuthService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
        when(authService.getAktorIdOrThrow(any(Fnr.class))).thenReturn(AktorId.of("test-id"));
        when(veilederTilordningerRepository.hentTilordningForAktoer(any()))
                .thenReturn(forventetIdent);

        final Veileder veileder = veilederController.getVeileder(Fnr.of("fnr"));
        assertNotNull(veileder);
        assertNotNull(veileder.getVeilederident());
        assertThat(veileder.getVeilederident(), is(forventetIdent));
    }
}
