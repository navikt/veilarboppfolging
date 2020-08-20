package no.nav.veilarboppfolging.domain.kafka;

import lombok.Value;

@Value
public class EndringPaManuellStatusKafkaDTO {
    private String aktorId;
    private boolean erManuell;
}
