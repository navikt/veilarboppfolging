package no.nav.fo.veilarboppfolging.mappers;

import lombok.*;

import java.time.ZonedDateTime;


@Getter
@Setter
public class ArenaBruker {
    public String aktoerid;
    public String fodselsnr;
    public String formidlingsgruppekode;
    public ZonedDateTime iserv_fra_dato;
}
