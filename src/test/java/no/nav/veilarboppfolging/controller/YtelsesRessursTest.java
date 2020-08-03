package no.nav.veilarboppfolging.controller;

import no.nav.veilarboppfolging.client.oppfolging.OppfolgingMapper;
import no.nav.veilarboppfolging.client.oppfolging.OppfolgingskontraktData;
import no.nav.veilarboppfolging.client.ytelseskontrakt.Vedtak;
import no.nav.veilarboppfolging.client.ytelseskontrakt.Ytelseskontrakt;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktMapper;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktResponse;
import no.nav.veilarboppfolging.controller.domain.YtelserResponse;
import no.nav.veilarboppfolging.service.AuthService;
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
    private OppfolgingMapper oppfolgingMapper;

    @Mock
    private AuthService authService;

    @BeforeClass
    public static void setup() {
        setProperty("no.nav.modig.security.systemuser.username", "username");
        setProperty("no.nav.modig.security.systemuser.password", "password");
    }

    @Test
    public void getOppfoelgingSkalReturnereEnRespons() {

        when(YtelseskontraktMapper.tilYtelseskontrakt(any())).thenReturn(
                new YtelseskontraktResponse(singletonList(new Vedtak()), singletonList(new Ytelseskontrakt()))
        );
        when(OppfolgingMapper.tilOppfolgingskontrakt(any())).thenReturn(singletonList(new OppfolgingskontraktData()));

        final YtelserResponse ytelser = ytelseController.getYtelser("fnr");

        assertThat(ytelser.getOppfoelgingskontrakter().isEmpty(), is(false));
        assertThat(ytelser.getVedtaksliste().isEmpty(), is(false));
        assertThat(ytelser.getYtelser().isEmpty(), is(false));
    }
}
