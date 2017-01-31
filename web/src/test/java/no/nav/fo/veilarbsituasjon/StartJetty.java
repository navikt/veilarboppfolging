package no.nav.fo.veilarbsituasjon;

import no.nav.modig.core.context.StaticSubjectHandler;
import no.nav.sbl.dialogarena.common.jetty.Jetty;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Properties;

import static java.lang.System.setProperty;
import static no.nav.modig.core.context.SubjectHandler.SUBJECTHANDLER_KEY;
import static no.nav.sbl.dialogarena.common.jetty.Jetty.usingWar;
import static no.nav.sbl.dialogarena.common.jetty.JettyStarterUtils.*;

class StartJetty {
    private static final int PORT = 8486;

    public static void main(String[] args) throws Exception {
        setProperty(SUBJECTHANDLER_KEY, StaticSubjectHandler.class.getName());

        Jetty jetty = usingWar()
                .at("/veilarbsituasjon")
                .port(PORT)
                .loadProperties("/jetty-test.properties")
                .overrideWebXml()
                .addDatasource(buildDataSource("oracledb.properties"), "jdbc/veilarbsituasjonDS")
                .buildJetty();
        jetty.startAnd(first(waitFor(gotKeypress())).then(jetty.stop));
    }

    public static DataSource buildDataSource(String propertyFileName) throws IOException {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();

        Properties env = dbProperties(propertyFileName);
        dataSource.setDriverClassName(env.getProperty("db.driverClassName"));
        dataSource.setUrl(env.getProperty("db.url"));
        dataSource.setUsername(env.getProperty("db.username"));
        dataSource.setPassword(env.getProperty("db.password"));
        return dataSource;
    }

    private static Properties dbProperties(String propertyFileName) throws IOException {
        Properties env = new Properties();
        env.load(StartJetty.class.getResourceAsStream("/" + propertyFileName));
        return env;
    }

}
