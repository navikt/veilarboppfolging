package no.nav.veilarboppfolging.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.aktoroppslag.AktorOppslagClient;
import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.selftest.SelfTestCheck;
import no.nav.common.health.selftest.SelfTestChecks;
import no.nav.common.health.selftest.SelfTestMeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
public class HelsesjekkConfig {

    @Bean
    public SelfTestChecks selfTestChecks(JdbcTemplate jdbcTemplate,
                                         AktorOppslagClient aktorOppslagClient) {
        List<SelfTestCheck> selfTestChecks = Arrays.asList(
                new SelfTestCheck("Enkel spørring mot Databasen til veilarboppfolging.", true, checkDbHealth(jdbcTemplate))
                // TODO selftest poao-tilgang
//                new SelfTestCheck("Ping av aktor oppslag client (konvertere mellom aktorId og Fnr).", true, aktorOppslagClient)
        );

        return new SelfTestChecks(selfTestChecks);
    }

    private HealthCheck checkDbHealth(JdbcTemplate jdbcTemplate) {
        return () -> {
            try {
                jdbcTemplate.queryForObject("SELECT 1", Long.class);
                return HealthCheckResult.healthy();
            } catch (Exception e) {
                log.error("Helsesjekk mot database feilet", e);
                return HealthCheckResult.unhealthy("Fikk ikke kontakt med databasen", e);
            }
        };
    }

    @Bean
    public SelfTestMeterBinder selfTestMeterBinder(SelfTestChecks selfTestChecks) {
        return new SelfTestMeterBinder(selfTestChecks);
    }
}
