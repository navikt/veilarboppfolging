package no.nav.veilarboppfolging.controller;

import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.veilarboppfolging.controller.response.Veileder;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.service.VeilederTilordningService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VeilederControllerTest {

    @InjectMocks
    private VeilederController veilederController;

    @Mock
    private VeilederTilordningService veilederTilordningService;

    @Mock
    private AuthService authService;

    @Test
    public void getVeilederSkalReturnereVeileder() {
        final NavIdent forventetIdent = NavIdent.of("Z4321");
        when(veilederTilordningService.hentTilordnetVeilederIdent(any()))
                .thenReturn(Optional.of(forventetIdent));

        final Veileder veileder = veilederController.getVeileder(Fnr.of("fnr"));
        assertNotNull(veileder);
        assertNotNull(veileder.getVeilederident());
        assertEquals(forventetIdent.get(), veileder.getVeilederident());
    }
}
