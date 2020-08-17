package no.nav.veilarboppfolging.controller.domain;

import lombok.Value;

import java.time.LocalDateTime;

@Value
public class OppfolgingStartetKafkaDTO {

    private String aktorId;
    private LocalDateTime oppfolgingStartet;

}
