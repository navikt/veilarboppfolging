package no.nav.fo.veilarbsituasjon;

import no.nav.fo.veilarbsituasjon.config.DatabaseLocalConfig;
import no.nav.fo.veilarbsituasjon.config.JndiLocalContextConfig;
import no.nav.modig.testcertificates.TestCertificates;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@ContextConfiguration(classes = DatabaseLocalConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public abstract class IntegrasjonsTest {

    @BeforeClass
    public static void testDatabase() throws IOException {
        JndiLocalContextConfig.setupInMemoryDatabase();
    }

}
