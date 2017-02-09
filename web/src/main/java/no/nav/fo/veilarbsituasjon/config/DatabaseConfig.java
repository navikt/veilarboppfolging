package no.nav.fo.veilarbsituasjon.config;

import no.nav.fo.veilarbsituasjon.domain.AktoerIdToVeileder;
import no.nav.sbl.dialogarena.common.integrasjon.utils.RowMapper;
import no.nav.sbl.dialogarena.common.integrasjon.utils.SQL;
import no.nav.sbl.dialogarena.types.Pingable;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.orm.hibernate4.LocalSessionFactoryBuilder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

    @Bean
    public DataSource dataSource() {
        JndiDataSourceLookup dataSourceLookup = new JndiDataSourceLookup();
        dataSourceLookup.setResourceRef(true);
        return dataSourceLookup.getDataSource("jdbc/veilarbsituasjonDS");
    }

    @Bean(name = "sessionFactory")
    public SessionFactory getSessionFactory() {
        LocalSessionFactoryBuilder sessionBuilder = new LocalSessionFactoryBuilder(dataSource());
        sessionBuilder.addAnnotatedClasses(AktoerIdToVeileder.class);
        sessionBuilder.addProperties(hibernateProperties());
        return sessionBuilder.buildSessionFactory();

    }

    @Bean
    @Autowired
    public HibernateTransactionManager transactionManager(SessionFactory sessionFactory) {
        HibernateTransactionManager txManager = new HibernateTransactionManager();
        txManager.setSessionFactory(sessionFactory);
        return txManager;
    }

    private Properties hibernateProperties() {
        Properties hibernate = new Properties();
        hibernate.setProperty("hibernate.dialect", "org.hibernate.dialect.Oracle10gDialect");
        hibernate.setProperty("hibernate.show_sql", "false");
        hibernate.setProperty("hibernate.format_sql", "true");
        hibernate.setProperty("hibernate.jdbc.fetch_size", "100");
        return hibernate;
    }

    @Bean
    public Pingable dbPinger(final DataSource ds) {
        return () -> {
            try {
                SQL.query(ds, new RowMapper.IntMapper(), "select count(1) from dual");
                return Pingable.Ping.lyktes("DATABASE");
            } catch (Exception e) {
                return Pingable.Ping.feilet("DATABASE", e);
            }
        };
    }
}
