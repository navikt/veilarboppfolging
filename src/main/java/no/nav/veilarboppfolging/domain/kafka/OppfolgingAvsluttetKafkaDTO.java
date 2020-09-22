package no.nav.veilarboppfolging.domain.kafka;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
public class OppfolgingAvsluttetKafkaDTO {
    private String aktorId;
    private ZonedDateTime sluttdato;
}
