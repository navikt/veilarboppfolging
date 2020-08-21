package no.nav.veilarboppfolging.controller.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class Mal {
    private String mal;
    private String endretAv;
    private Date dato;
}