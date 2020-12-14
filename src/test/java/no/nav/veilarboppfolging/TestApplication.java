package no.nav.veilarboppfolging;

import no.nav.veilarboppfolging.config.ApplicationTestConfig;
import no.nav.veilarboppfolging.test.LocalH2Database;
import no.nav.veilarboppfolging.test.testdriver.TestDriver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

@EnableAutoConfiguration
@Import(ApplicationTestConfig.class)
public class TestApplication {

    public static void main(String[] args) {
        // We need to initialize the driver before spring starts or Flyway will not be able to use the driver
        LocalH2Database.setUsePersistentDb();
        TestDriver.init();

        SpringApplication application = new SpringApplication(TestApplication.class);
        application.setAdditionalProfiles("local");
        application.run(args);
    }

}
