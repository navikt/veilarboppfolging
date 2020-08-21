package no.nav.veilarboppfolging.domain.kafka;

import lombok.Value;

@Value
public class EndringPaNyForVeilederKafkaDTO {
    private String aktorId;
    private boolean nyForVeileder;
}
