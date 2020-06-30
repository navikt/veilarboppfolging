package no.nav.veilarboppfolging.config;

import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.common.auth.Subject;
import no.nav.common.auth.SubjectHandler;
import no.nav.veilarboppfolging.services.*;
import no.nav.sbl.rest.RestUtils;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.OrganisasjonEnhetV2;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.client.*;
import java.io.IOException;

@Configuration
public class ServiceConfig {

    @Bean
    YtelseskontraktService ytelsesKontraktService(YtelseskontraktV3 ytelseskontraktV3) {
        return new YtelseskontraktService(ytelseskontraktV3);
    }

    @Bean
    OrganisasjonEnhetService organisasjonsenhetService(OrganisasjonEnhetV2 organisasjonEnhetV2) {
        return new OrganisasjonEnhetService(organisasjonEnhetV2);
    }

    @Bean
    ManuellStatusService manuellStatusService() {
        return new ManuellStatusService();
    }

    @Bean
    ArenaOppfolgingService arenaOppfolgingService(OppfoelgingPortType oppfoelgingPortType) {
        Client client = RestUtils.createClient();
        client.register(new SubjectOidcTokenFilter());
        return new ArenaOppfolgingService(oppfoelgingPortType, client);
    }

    @Bean
    public SystemUserTokenProvider systemUserTokenProvider() {
        return new SystemUserTokenProvider();
    }

    @Bean
    public OppfolgingsbrukerService oppfolgingsbrukerService(SystemUserTokenProvider systemUserTokenProvider) {
        Client client = RestUtils.createClient();
        client.register(new SystemUserOidcTokenProviderFilter(systemUserTokenProvider));
        return new OppfolgingsbrukerService(client);
    }

    private static class SystemUserOidcTokenProviderFilter implements ClientRequestFilter {
        private final SystemUserTokenProvider systemUserTokenProvider;

        private SystemUserOidcTokenProviderFilter(SystemUserTokenProvider systemUserTokenProvider) {
            this.systemUserTokenProvider = systemUserTokenProvider;
        }

        @Override
        public void filter(ClientRequestContext clientRequestContext) throws IOException {
            clientRequestContext.getHeaders().putSingle("Authorization", "Bearer " + systemUserTokenProvider.getToken());
        }
    }

    private static class SubjectOidcTokenFilter implements ClientRequestFilter {
        @Override
        public void filter(ClientRequestContext requestContext) {
            SubjectHandler.getSubject()
                    .map(Subject::getSsoToken)
                    .ifPresent(ssoToken ->
                            requestContext.getHeaders().putSingle("Authorization", "Bearer " + ssoToken.getToken()));

        }
    }
}
