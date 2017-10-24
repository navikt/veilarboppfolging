package no.nav.fo.veilarbsituasjon.ws.provider.oppfolgingsinfo;

import no.nav.brukerdialog.security.context.InternbrukerSubjectHandler;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.brukerdialog.security.oidc.TokenUtils;
import no.nav.brukerdialog.security.oidc.UserTokenProvider;
import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.tjeneste.virksomhet.oppfolgingsinfo.v1.OppfolgingsinfoV1;
import no.nav.tjeneste.virksomhet.oppfolgingsinfo.v1.meldinger.OppfolgingsstatusRequest;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static no.nav.dialogarena.config.DevelopmentSecurity.setupIntegrationTestSecurity;
import static no.nav.sbl.dialogarena.test.junit.Tag.SMOKETEST;

@Tag(SMOKETEST)
public class OppfolgingsinfoSmoketest {

    private static final String AREMARK_AKTORID = "***REMOVED***42";
    public static final String SMOKETEST = "smoketest";
    private OppfolgingsinfoV1 oppfolgingsinfoV1;
    private static String hostname;
    private static String MILJO;

    @BeforeAll
    public static void setup() {
        MILJO = getProperty("miljo");
        setupIntegrationTestSecurity(new DevelopmentSecurity.IntegrationTestConfig("veilarbportefolje"));
        setProperty("no.nav.brukerdialog.security.context.subjectHandlerImplementationClass", InternbrukerSubjectHandler.class.getName());
    }

    @BeforeEach
    public void before() throws Exception {
        hostname = Objects.nonNull(MILJO) ? String.format("https://app-%s.adeo.no/", MILJO) : "http://localhost:8080/";
        oppfolgingsinfoV1 = new CXFClient<>(OppfolgingsinfoV1.class)
                .withOutInterceptor(new LoggingOutInterceptor())
                .address(hostname + "veilarbsituasjon-ws/ws/oppfolgingsinfo")
                .configureStsForOnBehalfOfWithJWT()
                .build();

        UserTokenProvider userTokenProvider = new UserTokenProvider();
        OidcCredential token = userTokenProvider.getIdToken();
        InternbrukerSubjectHandler.setOidcCredential(token);
        InternbrukerSubjectHandler.setVeilederIdent(TokenUtils.getTokenSub(token.getToken()));
    }

    @Test
    public void hentOppfolgingsinfo() {
        OppfolgingsstatusRequest request = new OppfolgingsstatusRequest().withAktorId(AREMARK_AKTORID);
        oppfolgingsinfoV1.hentOppfolgingsstatus(request);
    }
}
