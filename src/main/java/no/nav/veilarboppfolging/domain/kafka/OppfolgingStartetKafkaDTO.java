package no.nav.veilarboppfolging.domain.kafka;

import lombok.Value;

import java.time.ZonedDateTime;

@Value
public class OppfolgingStartetKafkaDTO {
    private String aktorId;
    private ZonedDateTime oppfolgingStartet;
}
