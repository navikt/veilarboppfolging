package no.nav.fo.veilarbsituasjon.config.mq;

import org.springframework.context.annotation.Bean;
import org.springframework.jms.core.JmsTemplate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.naming.InitialContext;
import javax.naming.NamingException;


public class SituasjonMQConfig {

    @Inject
    @Named("connectionFactory")
    private ConnectionFactory connectionFactory;

    @Bean(name = "endreVeilederKo")
    public JmsTemplate endreVeilederQueue() throws NamingException {
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setDefaultDestination(endreVeilederKo());
        jmsTemplate.setConnectionFactory(connectionFactory);
        return jmsTemplate;
    }

    @Bean
    public Destination endreVeilederKo() throws NamingException {
        return (Destination) new InitialContext().lookup("java:jboss/jms/endreVeilederKo");
    }
}

