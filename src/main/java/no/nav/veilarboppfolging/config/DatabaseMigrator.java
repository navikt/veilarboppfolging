package no.nav.veilarboppfolging.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.featuretoggle.UnleashClient;
import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseMigrator {

    public static final String FLYWAY_REPAIR_TOGGLE = "veilarboppfolging.flyway.repair";

    private final DataSource dataSource;
    private final UnleashClient unleashClient;


    @PostConstruct
    public void migrateDb() {
        log.info("Starting database migration...");
        var flyway = new Flyway(Flyway.configure()
                .dataSource(dataSource)
                .table("schema_version"));
        if (unleashClient.isEnabled(FLYWAY_REPAIR_TOGGLE)) {
            List<String> warnings = flyway.info().getInfoResult().warnings;
            log.warn("Flyway warnings: {}", warnings);
            log.warn("Toggle for flyway repair aktiv. Kjører flyway repair før migrate");
            flyway.repair();
        }
        flyway.migrate();
    }

}
