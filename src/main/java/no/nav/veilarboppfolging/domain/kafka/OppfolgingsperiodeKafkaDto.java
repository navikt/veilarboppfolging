package no.nav.veilarboppfolging.domain.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.common.types.identer.AktorId;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OppfolgingsperiodeKafkaDto {
    UUID uuid;
    AktorId aktorId;
    ZonedDateTime startDato;
    ZonedDateTime sluttDato;
}
