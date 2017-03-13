package no.nav.fo.veilarbsituasjon.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;


import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.naming.NamingException;

@Configuration
public class MessageQueueMockConfig {

    public static void setupBrokerService() throws Exception {
        final BrokerService broker = new BrokerService();
        broker.getSystemUsage().getTempUsage().setLimit(100 * 1024 * 1024 * 100);
        broker.getSystemUsage().getStoreUsage().setLimit(100 * 1024 * 1024 * 100);
        broker.addConnector("tcp://localhost:61616");
        broker.start();
    }

    @Bean(name = "jmsListenerContainerFactory")
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory() throws NamingException {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory());
        factory.setDestinationResolver((session, s, b) -> {
            try {
                return portefoljeKo();
            } catch (NamingException e) {
                e.printStackTrace();
            }
            return null;
        });
        factory.setConcurrency("3-10");
        return factory;
    }

    @Bean
    public ConnectionFactory connectionFactory() throws NamingException {
        return new ActiveMQConnectionFactory("tcp://localhost:61616");
    }


    @Bean
    public Destination portefoljeKo() throws NamingException {
        return new ActiveMQQueue("portefolje");
    }

    @Bean(name = "endreveilederko")
    public JmsTemplate portefoljequeue() throws NamingException {
        JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory());
        jmsTemplate.setConnectionFactory(connectionFactory());
        jmsTemplate.setDefaultDestination(portefoljeKo());
        return jmsTemplate;
    }

}
