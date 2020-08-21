package no.nav.veilarboppfolging.domain.kafka;

import lombok.Value;

@Value
public class VeilederTilordnetKafkaDTO {
    private String aktorId;
    private String veilederId;
}
