package no.nav.fo.veilarbsituasjon.utils;

import static java.util.UUID.randomUUID;

import org.springframework.jms.core.MessageCreator;

import javax.jms.TextMessage;

public class JmsUtil {

    public static MessageCreator messageCreator(final String hendelse) {
        return session -> {
            TextMessage msg = session.createTextMessage(hendelse);
            msg.setStringProperty("callId", randomUUID().toString());
            return msg;
        };
    }
}
