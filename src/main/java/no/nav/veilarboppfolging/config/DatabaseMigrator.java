package no.nav.veilarboppfolging.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;

@Slf4j
@Component
public class DatabaseMigrator {

    private final DataSource dataSource;

    @Autowired
    public DatabaseMigrator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void migrateDb() {
        log.info("Starting database migration...");
        var flyway = new Flyway(Flyway.configure()
                .dataSource(dataSource)
                .table("schema_version")
                .validateMigrationNaming(true));
        flyway.migrate();
    }

}
