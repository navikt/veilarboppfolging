package no.nav.fo.veilarbsituasjon.config;

import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class JndiLocalContextConfig {
    public static void setupJndiLocalContext() {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.eclipse.jetty.jndi.InitialContextFactory");
        System.setProperty(Context.URL_PKG_PREFIXES, "org.apache.naming");

        try {
            SingleConnectionDataSource ds = new SingleConnectionDataSource();
            ds.setUrl("jdbc:oracle:thin:@d26dbfl007.test.local:1521/t4_veilarbsituasjon");
            ds.setUsername("t4_veilarbsituasjon");
            ds.setPassword("Change ME!");
            ds.setSuppressClose(true);

            InitialContext ctx = new InitialContext();
            ctx.createSubcontext("java:/");
            ctx.createSubcontext("java:/jboss/");
            ctx.createSubcontext("java:/jboss/jdbc/");

            ctx.bind("java:/jboss/jdbc/veilarbsituasjonDS", ds);

        } catch (NamingException e) {
            System.out.printf(e.toString());
        }
    }
}
