package no.nav.veilarboppfolging.controller.response;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
public class Maal {
    private String mal;
    private String endretAv;
    private ZonedDateTime dato;
}