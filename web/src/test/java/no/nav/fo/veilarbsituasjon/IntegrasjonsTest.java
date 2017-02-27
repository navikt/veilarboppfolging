package no.nav.fo.veilarbsituasjon;

import no.nav.fo.veilarbsituasjon.config.DatabaseLocalConfig;
import no.nav.fo.veilarbsituasjon.config.JndiLocalContextConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;

public abstract class IntegrasjonsTest {

    protected static AnnotationConfigApplicationContext annotationConfigApplicationContext;

    @BeforeAll
    public static void setup() throws IOException {
        JndiLocalContextConfig.setupInMemoryDatabase();
        annotationConfigApplicationContext = new AnnotationConfigApplicationContext(DatabaseLocalConfig.class);
        annotationConfigApplicationContext.start();
    }

    @AfterAll
    public static void tearDown() {
        annotationConfigApplicationContext.stop();
    }

    protected static JdbcTemplate getBean(Class<JdbcTemplate> requiredType) {
        return annotationConfigApplicationContext.getBean(requiredType);
    }

}
