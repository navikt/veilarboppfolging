package no.nav.veilarboppfolging.controller;

import no.nav.apiapp.security.PepClient;
import no.nav.veilarboppfolging.domain.OppfolgingskontraktData;
import no.nav.veilarboppfolging.domain.OppfolgingskontraktResponse;
import no.nav.veilarboppfolging.services.AuthService;
import no.nav.veilarboppfolging.utils.mappers.OppfolgingMapper;
import no.nav.veilarboppfolging.utils.mappers.YtelseskontraktMapper;
import no.nav.veilarboppfolging.controller.domain.Vedtak;
import no.nav.veilarboppfolging.controller.domain.YtelserResponse;
import no.nav.veilarboppfolging.controller.domain.Ytelseskontrakt;
import no.nav.veilarboppfolging.controller.domain.YtelseskontraktResponse;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.lang.System.setProperty;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class YtelsesRessursTest {

    @InjectMocks
    private YtelseController ytelseController;

    @Mock
    private YtelseskontraktMapper ytelseskontraktMapper;

    @Mock
    @SuppressWarnings("unused")
    private YtelseskontraktService ytelseskontraktService;

    @Mock
    private OppfolgingMapper oppfolgingMapper;

    @Mock
    @SuppressWarnings("unused")
    private ArenaOppfolgingService arenaOppfolgingService;

    @Mock
    @SuppressWarnings("unused")
    private PepClient pepClient;

    @Mock
    private AuthService authService;

    @BeforeClass
    public static void setup() {
        setProperty("no.nav.modig.security.systemuser.username", "username");
        setProperty("no.nav.modig.security.systemuser.password", "password");
    }

    @Test
    public void getOppfoelgingSkalReturnereEnRespons() throws Exception {

        when(ytelseskontraktMapper.tilYtelseskontrakt(any())).thenReturn(
                new YtelseskontraktResponse(singletonList(new Vedtak()), singletonList(new Ytelseskontrakt()))
        );
        when(oppfolgingMapper.tilOppfolgingskontrakt(any())).thenReturn(
                new OppfolgingskontraktResponse(singletonList(new OppfolgingskontraktData()))
        );

        final YtelserResponse ytelser = ytelseController.getYtelser("fnr");

        assertThat(ytelser.getOppfoelgingskontrakter().isEmpty(), is(false));
        assertThat(ytelser.getVedtaksliste().isEmpty(), is(false));
        assertThat(ytelser.getYtelser().isEmpty(), is(false));
    }
}
