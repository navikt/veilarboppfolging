package no.nav.fo.veilarboppfolging.rest;

import no.nav.apiapp.security.PepClient;
import no.nav.brukerdialog.security.context.SubjectHandlerUtils;
import no.nav.brukerdialog.security.context.ThreadLocalSubjectHandler;
import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.fo.veilarboppfolging.domain.OppfolgingskontraktData;
import no.nav.fo.veilarboppfolging.domain.OppfolgingskontraktResponse;
import no.nav.fo.veilarboppfolging.mappers.OppfolgingMapper;
import no.nav.fo.veilarboppfolging.mappers.YtelseskontraktMapper;
import no.nav.fo.veilarboppfolging.rest.domain.Vedtak;
import no.nav.fo.veilarboppfolging.rest.domain.YtelserResponse;
import no.nav.fo.veilarboppfolging.rest.domain.Ytelseskontrakt;
import no.nav.fo.veilarboppfolging.rest.domain.YtelseskontraktResponse;
import no.nav.fo.veilarboppfolging.services.OppfolgingService;
import no.nav.fo.veilarboppfolging.services.YtelseskontraktService;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static java.lang.System.setProperty;
import static java.util.Collections.singletonList;
import static no.nav.brukerdialog.security.context.SubjectHandler.SUBJECTHANDLER_KEY;
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
    private OppfolgingService oppfolgingService;

    @Mock
    PepClient pepClient;

    @BeforeClass
    public static void setup() {
        setProperty("no.nav.modig.security.systemuser.username", "username");
        setProperty("no.nav.modig.security.systemuser.password", "password");
        setProperty(SUBJECTHANDLER_KEY, ThreadLocalSubjectHandler.class.getName());
        SubjectHandlerUtils.setSubject(new SubjectHandlerUtils.SubjectBuilder("userId", IdentType.InternBruker).withAuthLevel(3).getSubject());
    }

    @Test
    public void getOppfoelgingSkalReturnereEnRespons() throws Exception {

        when(ytelseskontraktMapper.tilYtelseskontrakt(any())).thenReturn(
                new YtelseskontraktResponse(singletonList(new Vedtak()), singletonList(new Ytelseskontrakt()))
        );
        when(oppfolgingMapper.tilOppfolgingskontrakt(any())).thenReturn(
                new OppfolgingskontraktResponse(singletonList(new OppfolgingskontraktData()))
        );

        final YtelserResponse ytelser = ytelseRessurs.getYtelser("***REMOVED***");

        assertThat(ytelser.getOppfoelgingskontrakter().isEmpty(), is(false));
        assertThat(ytelser.getVedtaksliste().isEmpty(), is(false));
        assertThat(ytelser.getYtelser().isEmpty(), is(false));
    }
}
