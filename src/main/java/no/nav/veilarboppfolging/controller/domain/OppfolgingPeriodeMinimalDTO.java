package no.nav.veilarboppfolging.controller.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
public class OppfolgingPeriodeMinimalDTO {
    private String id;
    private ZonedDateTime startDato;
    private ZonedDateTime sluttDato;
}
