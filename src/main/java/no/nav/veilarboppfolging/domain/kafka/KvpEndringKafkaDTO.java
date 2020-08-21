package no.nav.veilarboppfolging.domain.kafka;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
public class KvpEndringKafkaDTO {

    private String aktorId;
    private String enhetId;

    private String opprettetAv;
    private ZonedDateTime opprettetDato;
    private String opprettetBegrunnelse;

    private String avsluttetAv;
    private ZonedDateTime avsluttetDato;
    private String avsluttetBegrunnelse;

}
