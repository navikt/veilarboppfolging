package no.nav.veilarboppfolging.domain.kafka;

import lombok.Value;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.NavIdent;

import java.time.ZonedDateTime;

@Value
public class SisteTilordnetVeilederKafkaDTO {
    AktorId aktorId;
    NavIdent veilederId;
    ZonedDateTime tilordnet;
}
