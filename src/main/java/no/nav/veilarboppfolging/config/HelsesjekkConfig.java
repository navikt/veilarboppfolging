package no.nav.veilarboppfolging.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.common.abac.Pep;
import no.nav.common.client.aktorregister.AktorregisterClient;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.health.HealthCheck;
import no.nav.common.health.HealthCheckResult;
import no.nav.common.health.selftest.SelfTestCheck;
import no.nav.common.health.selftest.SelfTestChecks;
import no.nav.common.health.selftest.SelfTestMeterBinder;
import no.nav.veilarboppfolging.client.behandle_arbeidssoker.BehandleArbeidssokerClient;
import no.nav.veilarboppfolging.client.oppfolging.OppfolgingClient;
import no.nav.veilarboppfolging.client.varseloppgave.VarseloppgaveClient;
import no.nav.veilarboppfolging.client.ytelseskontrakt.YtelseskontraktClient;
import no.nav.veilarboppfolging.kafka.KafkaHelsesjekk;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
public class HelsesjekkConfig {

    @Bean
    public SelfTestChecks selfTestChecks(KafkaHelsesjekk kafkaHelsesjekk,
                                         BehandleArbeidssokerClient behandleArbeidssokerClient,
                                         YtelseskontraktClient ytelseskontraktClient,
                                         OppfolgingClient oppfolgingClient,
                                         JdbcTemplate jdbcTemplate,
                                         Pep pep,
                                         VarseloppgaveClient varseloppgaveClient,
                                         AktorregisterClient aktorregisterClient,
                                         UnleashService unleashService) {
        List<SelfTestCheck> selfTestChecks = Arrays.asList(
                new SelfTestCheck("Kafka consumer", false, kafkaHelsesjekk),
                new SelfTestCheck("Ping av BehandleArbeidssoker_V1. Registrerer arbeidssoker i Arena.", true, behandleArbeidssokerClient),
                new SelfTestCheck("Ping av ytelseskontrakt_V3. Henter informasjon om ytelser fra Arena.", false, ytelseskontraktClient),
                new SelfTestCheck("Ping av oppfolging_v1. Henter informasjon om oppfølgingsstatus fra arena.", true, oppfolgingClient),
                new SelfTestCheck("Enkel spørring mot Databasen til veilarboppfolging.", true, checkDbHealth(jdbcTemplate)),
                new SelfTestCheck("ABAC tilgangskontroll - ping", true, pep.getAbacClient()),
                new SelfTestCheck("Brukes for å sende eskaleringsvarsel", false, varseloppgaveClient),
                new SelfTestCheck("Ping av aktorregister (konvertere mellom aktorId og Fnr).", true, aktorregisterClient),
                new SelfTestCheck("Sjekker at feature-toggles kan hentes fra Unleash", false, unleashService)
        );

        return new SelfTestChecks(selfTestChecks);
    }

    private HealthCheck checkDbHealth(JdbcTemplate jdbcTemplate) {
        return () -> {
            try {
                jdbcTemplate.queryForObject("SELECT 1 FROM DUAL", Long.class);
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
