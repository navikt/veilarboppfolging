package no.nav.fo.veilarbsituasjon.utils;

import org.springframework.jms.core.MessageCreator;

import javax.jms.TextMessage;

public class JmsUtil {

    public static MessageCreator messageCreator(final String hendelse, final String callId) {
        return session -> {
            TextMessage msg = session.createTextMessage(hendelse);
            msg.setStringProperty("callId", callId);
            return msg;
        };
    }
}
