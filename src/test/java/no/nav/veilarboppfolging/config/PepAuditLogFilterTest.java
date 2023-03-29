package no.nav.veilarboppfolging.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import no.nav.common.abac.VeilarbPep;
import no.nav.common.abac.domain.request.ActionId;
import no.nav.common.types.identer.EnhetId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.types.identer.NavIdent;
import no.nav.common.utils.Credentials;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static jakarta.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.common.abac.constants.NavAttributter.RESOURCE_VEILARB_ENHET_EIENDEL;
import static no.nav.common.rest.filter.LogRequestFilter.NAV_CONSUMER_ID_HEADER_NAME;
import static no.nav.common.utils.EnvironmentUtils.NAIS_APP_NAME_PROPERTY_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PepAuditLogFilterTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Test
    public void riktig_filtrering_av_audit_logging() {

        mockRequestContextHolder();
        System.setProperty(NAIS_APP_NAME_PROPERTY_NAME, "test");

        String url = "http://localhost:" + wireMockRule.port();
        List<ILoggingEvent> logsList = logEventsForLogger("AuditLogger");

        givenAbacPermitResponse();

        ApplicationConfig applicationConfig = new ApplicationConfig();

        EnvironmentProperties environmentProperties = new EnvironmentProperties();
        environmentProperties.setAbacUrl(url);

        VeilarbPep pep = (VeilarbPep) applicationConfig.veilarbPep(environmentProperties, new Credentials("", ""));

        pep.harTilgangTilEnhet("", EnhetId.of(""));
        pep.harTilgangTilEnhetMedSperre("", EnhetId.of(""));
        pep.harTilgangTilPerson("", ActionId.READ, Fnr.of(""));
        pep.harVeilederTilgangTilPerson(NavIdent.of(""), ActionId.READ, Fnr.of(""));


        assertEquals(3, logsList.size());
        assertFalse("Skal ikke logge " + RESOURCE_VEILARB_ENHET_EIENDEL,
                logsList.stream().anyMatch(iLoggingEvent -> iLoggingEvent.toString().contains(RESOURCE_VEILARB_ENHET_EIENDEL)));
    }

    private void mockRequestContextHolder() {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getHeader(NAV_CONSUMER_ID_HEADER_NAME)).thenReturn("123");
        when(httpServletRequest.getMethod()).thenReturn("GET");
        when(httpServletRequest.getRequestURI()).thenReturn("/");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpServletRequest));
    }

    private void givenAbacPermitResponse() {
        givenThat(post(urlEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody("""
                                {
                                  "Response": {
                                    "Decision": "Permit"
                                  }
                                }""")));
    }

    private List<ILoggingEvent> logEventsForLogger(String loggerName) {
        Logger logger = (Logger) LoggerFactory.getLogger(loggerName);

        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();

        logger.addAppender(listAppender);

        return listAppender.list;
    }

}
