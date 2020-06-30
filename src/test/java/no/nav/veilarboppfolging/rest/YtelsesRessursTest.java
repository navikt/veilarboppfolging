package no.nav.veilarboppfolging.rest;

import no.nav.apiapp.security.PepClient;
import no.nav.veilarboppfolging.domain.OppfolgingskontraktData;
import no.nav.veilarboppfolging.domain.OppfolgingskontraktResponse;
import no.nav.veilarboppfolging.mappers.OppfolgingMapper;
import no.nav.veilarboppfolging.mappers.YtelseskontraktMapper;
import no.nav.veilarboppfolging.rest.domain.Vedtak;
import no.nav.veilarboppfolging.rest.domain.YtelserResponse;
import no.nav.veilarboppfolging.rest.domain.Ytelseskontrakt;
import no.nav.veilarboppfolging.rest.domain.YtelseskontraktResponse;
import no.nav.veilarboppfolging.services.ArenaOppfolgingService;
import no.nav.veilarboppfolging.services.YtelseskontraktService;
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
    private YtelseRessurs ytelseRessurs;

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
    private AutorisasjonService autorisasjonService;

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

        final YtelserResponse ytelser = ytelseRessurs.getYtelser("fnr");

        assertThat(ytelser.getOppfoelgingskontrakter().isEmpty(), is(false));
        assertThat(ytelser.getVedtaksliste().isEmpty(), is(false));
        assertThat(ytelser.getYtelser().isEmpty(), is(false));
    }
}
