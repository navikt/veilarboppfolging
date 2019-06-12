package no.nav.fo.veilarboppfolging.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
@Data
@Accessors(chain = true)
public class AvsluttOppfolgingKafkaDTO {
    private String aktorId;
    private Date sluttdato;
}
