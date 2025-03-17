package no.nav.veilarboppfolging.repository.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class KafkaMicrofrontendEntity {
    String aktorId;
    KafkaMicrofrontendStatus status;
    ZonedDateTime dato;
    String melding;
}

