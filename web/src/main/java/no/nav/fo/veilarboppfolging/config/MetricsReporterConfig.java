package no.nav.fo.veilarboppfolging.config;

import no.nav.metrics.Event;
import no.nav.metrics.MetricsFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import javax.inject.Inject;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Configuration
public class MetricsReporterConfig {

    private static final int MINUTE = 60 * 1000;

    @Inject
    private JdbcTemplate jdbc;

    @Value("${metrikk.for.veilederusynk:true}")
    private boolean rapporterMetrikkForVeilederUsynk;

    @Scheduled(fixedDelay = MINUTE)
    public void reportMetrics() {
        if(rapporterMetrikkForVeilederUsynk) {
            antallVeiledereIkkeOverfortTilPortefolje();
        }
    }

    private void antallVeiledereIkkeOverfortTilPortefolje() {
        String sql = Stream.of(
                "SELECT count(*) as antallIUsynk",
                "FROM OPPFOLGINGSTATUS os",
                "LEFT JOIN OPPFOLGING_DATA@VEILARBPORTEFOLJE bd ON (os.AKTOR_ID = bd.AKTOERID)",
                "WHERE os.OPPDATERT < (sysdate - 1/24)",
                "AND (",
                "    (os.VEILEDER is null AND bd.veilederident is not null) OR",
                "    (os.VEILEDER is not null AND bd.veilederident is null) OR",
                "    (os.VEILEDER != bd.veilederident)",
                ")"
        ).collect(Collectors.joining(" "));

        Integer antallIUsynkMedPortefolje = jdbc.queryForObject(sql, Integer.class);

        Event event = MetricsFactory.createEvent("veilarboppfolging.antallIUsynkMedPortefolje");
        event.addFieldToReport("count", antallIUsynkMedPortefolje);
        event.report();
    }

}
