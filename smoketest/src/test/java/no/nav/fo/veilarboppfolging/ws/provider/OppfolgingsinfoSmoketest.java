package no.nav.fo.veilarboppfolging.ws.provider;

import no.nav.brukerdialog.security.domain.IdentType;
import no.nav.brukerdialog.security.domain.OidcCredential;
import no.nav.brukerdialog.security.oidc.UserTokenProvider;
import no.nav.common.auth.SsoToken;
import no.nav.common.auth.Subject;
import no.nav.common.auth.SubjectHandler;
import no.nav.dialogarena.config.DevelopmentSecurity;
import no.nav.sbl.dialogarena.common.cxf.CXFClient;
import no.nav.sbl.util.EnvironmentUtils;
import no.nav.tjeneste.virksomhet.oppfolgingsinfo.v1.OppfolgingsinfoV1;
import no.nav.tjeneste.virksomhet.oppfolgingsinfo.v1.meldinger.OppfolgingsstatusRequest;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static no.nav.dialogarena.config.DevelopmentSecurity.setupIntegrationTestSecurity;
import static no.nav.sbl.dialogarena.test.junit.Tag.SMOKETEST;

@Tag(SMOKETEST)
public class OppfolgingsinfoSmoketest {

    private static final String AREMARK_AKTORID = "***REMOVED***42";
    private OppfolgingsinfoV1 oppfolgingsinfoV1;
    private static String hostname;
    private static String MILJO;

    @BeforeAll
    public static void setup() {
        MILJO = EnvironmentUtils.getOptionalProperty("miljo").orElse(null);
        setupIntegrationTestSecurity(new DevelopmentSecurity.IntegrationTestConfig("veilarboppfolging"));
    }

    @BeforeEach
    public void before() throws Exception {
        hostname = Objects.nonNull(MILJO) ? String.format("https://app-%s.adeo.no/", MILJO) : "https://localhost:8080/";
        oppfolgingsinfoV1 = new CXFClient<>(OppfolgingsinfoV1.class)
                .withOutInterceptor(new LoggingOutInterceptor())
                .address(hostname + "veilarboppfolging-ws/ws/oppfolgingsinfo")
                .configureStsForOnBehalfOfWithJWT()
                .build();
    }

    @Test
    public void hentOppfolgingsinfo() {
        UserTokenProvider userTokenProvider = new UserTokenProvider();
        OidcCredential token = userTokenProvider.getIdToken();
        Subject subject = new Subject("uid", IdentType.InternBruker, SsoToken.oidcToken(token.getToken()));
        SubjectHandler.withSubject(subject, () -> {
            OppfolgingsstatusRequest request = new OppfolgingsstatusRequest().withAktorId(AREMARK_AKTORID);
            oppfolgingsinfoV1.hentOppfolgingsstatus(request);
        });
    }
}
