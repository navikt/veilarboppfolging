package no.nav.veilarboppfolging.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FeiletKafkaMelding {
    long id;
    String topicName;
    String messageKey;
    String jsonPayload;
}
