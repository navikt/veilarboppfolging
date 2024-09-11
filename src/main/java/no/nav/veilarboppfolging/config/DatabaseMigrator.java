package no.nav.veilarboppfolging.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseMigrator {

    private final DataSource flywayDataSource;

    @PostConstruct
    public void migrateDb() {
        log.info("Starting database migration...");
        var flyway = new Flyway(Flyway.configure()
                .dataSource(flywayDataSource)
                .table("schema_version"));
        flyway.migrate();
    }

}
