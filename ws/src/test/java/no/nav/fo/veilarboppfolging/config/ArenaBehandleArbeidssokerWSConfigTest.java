package no.nav.fo.veilarboppfolging.config;

import no.nav.sbl.dialogarena.common.cxf.StsSecurityConstants;
import no.nav.sbl.dialogarena.types.Pingable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static no.nav.fo.veilarboppfolging.config.ArenaBehandleArbeidssokerWSConfig.FEATURE_SKIP_REGISTRER_BRUKER_PROPERTY;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class ArenaBehandleArbeidssokerWSConfigTest {

    static AnnotationConfigApplicationContext context;


    @BeforeAll
    public static void setupAll() {
        System.setProperty(StsSecurityConstants.STS_URL_KEY, "");
        System.setProperty(StsSecurityConstants.SYSTEMUSER_USERNAME, "");
        System.setProperty(StsSecurityConstants.SYSTEMUSER_PASSWORD, "");
    }

    @BeforeEach
    public void setup() {
        System.clearProperty(FEATURE_SKIP_REGISTRER_BRUKER_PROPERTY);
    }

    @AfterAll
    public void tearDown() {
        if(context != null) {
            context.stop();
        }
    }


    @Test
    public void skalIkkeConfigurerPingableNaarFatureIkkeErSatt() {
        context = new AnnotationConfigApplicationContext(ArenaBehandleArbeidssokerWSConfig.class);
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(Pingable.class));
    }

    @Test
    public void skalIkkeConfigurerPingableNaarFatureErAv() {
        System.setProperty(FEATURE_SKIP_REGISTRER_BRUKER_PROPERTY, "true");
        context = new AnnotationConfigApplicationContext(ArenaBehandleArbeidssokerWSConfig.class);
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(Pingable.class));
    }

    @Test
    public void skalConfigurerPingableNaarFatureErPaa() {
        System.setProperty(FEATURE_SKIP_REGISTRER_BRUKER_PROPERTY, "false");
        context = new AnnotationConfigApplicationContext(ArenaBehandleArbeidssokerWSConfig.class);
        Pingable aktiverArbeidssokerPing = context.getBean(Pingable.class);
        assertThat(aktiverArbeidssokerPing).isInstanceOf(Pingable.class);
    }
}