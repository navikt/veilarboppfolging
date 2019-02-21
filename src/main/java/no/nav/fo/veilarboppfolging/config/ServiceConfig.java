package no.nav.fo.veilarboppfolging.config;

import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import net.javacrumbs.shedlock.provider.jdbc.JdbcLockProvider;
import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.fo.veilarboppfolging.db.*;
import no.nav.fo.veilarboppfolging.services.*;
import no.nav.sbl.jdbc.Database;
import no.nav.sbl.rest.RestUtils;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import no.nav.tjeneste.virksomhet.oppfoelgingsstatus.v2.binding.OppfoelgingsstatusV2;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.binding.OrganisasjonEnhetV2;
import no.nav.tjeneste.virksomhet.ytelseskontrakt.v3.YtelseskontraktV3;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
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
    ArenaOppfolgingService arenaOppfolgingService(OppfoelgingsstatusV2 oppfoelgingsstatusV2,
                                                  OppfoelgingPortType oppfoelgingPortType) {

        return new ArenaOppfolgingService(oppfoelgingsstatusV2, oppfoelgingPortType);
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


}