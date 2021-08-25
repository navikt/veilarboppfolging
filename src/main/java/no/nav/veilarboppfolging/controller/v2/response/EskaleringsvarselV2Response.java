package no.nav.veilarboppfolging.controller.v2.response;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
public class EskaleringsvarselV2Response {
    long varselId;

    String opprettetAv;
    ZonedDateTime opprettetDato;
    String opprettetBegrunnelse;

    ZonedDateTime avsluttetDato;
    String avsluttetAv;
    String avsluttetBegrunnelse;

    long tilhorendeDialogId;
}
