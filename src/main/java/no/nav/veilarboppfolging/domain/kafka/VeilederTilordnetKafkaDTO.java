package no.nav.veilarboppfolging.domain.kafka;

import lombok.Value;

@Value
public class VeilederTilordnetKafkaDTO {
    String aktorId;
    String veilederId;
}
