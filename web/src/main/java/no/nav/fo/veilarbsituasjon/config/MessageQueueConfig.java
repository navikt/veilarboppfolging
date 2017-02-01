package no.nav.fo.veilarbsituasjon.config;

import no.nav.fo.veilarbsituasjon.config.mq.SituasjonMQConfig;
import no.nav.sbl.dialogarena.types.Pingable;
import org.slf4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import static no.nav.sbl.dialogarena.types.Pingable.Ping.feilet;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.lyktes;
import static org.slf4j.LoggerFactory.getLogger;

@Configuration
@EnableJms
@Import({
        SituasjonMQConfig.class
})
public class MessageQueueConfig {

    private static final Logger LOG = getLogger(MessageQueueConfig.class);

    @Bean
    public PlatformTransactionManager transactionManager() {
        return new JtaTransactionManager();
    }

    @Bean(name = "jmsListenerContainerFactory")
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory() throws NamingException {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory());
        factory.setDestinationResolver(destinationResolver());
        factory.setConcurrency("3-10");
        factory.setTransactionManager(transactionManager());
        return factory;
    }

    @Bean
    public ConnectionFactory connectionFactory() throws NamingException {
        return (ConnectionFactory) new InitialContext().lookup("java:jboss/mqConnectionFactory");
    }

    @Bean
    public DestinationResolver destinationResolver() {
        return (session, destinationName, pubSubDomain) -> {
            try {
                return (Destination) new InitialContext().lookup(destinationName);
            } catch (NamingException e) {
                LOG.error("Feil i DestinationResolver", e);
            }
            return null;
        };
    }

    @Bean
    public Pingable.Ping ping() {
        try {
            connectionFactory().createConnection().close();
            return lyktes("ENDREVEILEDER");
        } catch (Exception e) {
            return feilet("ENDREVEILEDER", e);
        }
    }
}