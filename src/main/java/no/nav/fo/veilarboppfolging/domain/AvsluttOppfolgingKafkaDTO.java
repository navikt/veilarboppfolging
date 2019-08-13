package no.nav.fo.veilarboppfolging.domain;

import lombok.Data;
import lombok.experimental.Accessors;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class AvsluttOppfolgingKafkaDTO {
    private String aktorId;
    private LocalDateTime sluttdato;
}
