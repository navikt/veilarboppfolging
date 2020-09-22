package no.nav.veilarboppfolging.controller.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
public class Mal {
    private String mal;
    private String endretAv;
    private ZonedDateTime dato;
}