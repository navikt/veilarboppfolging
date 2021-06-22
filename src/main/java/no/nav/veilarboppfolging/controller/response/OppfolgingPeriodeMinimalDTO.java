package no.nav.veilarboppfolging.controller.response;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Accessors(chain = true)
public class OppfolgingPeriodeMinimalDTO {
    private UUID uuid;
    private ZonedDateTime startDato;
    private ZonedDateTime sluttDato;
}
