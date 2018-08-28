package no.nav.fo.veilarboppfolging.mappers;

import lombok.*;

import java.time.ZonedDateTime;


@Getter
@Setter
public class ArenaBruker {

    public String aktoerid;
    public String formidlingsgruppekode;
    public String iserv_fra_dato;
    String fodselsnr;
    String etternavn;
    String fornavn;
    String nav_kontor;
    String kvalifiseringsgruppekode;
    String rettighetsgruppekode;
    String hovedmaalkode;
    String sikkerhetstiltak_type_kode;
    String fr_kode;
    Boolean har_oppfolgingssak;
    String sperret_ansatt;
    Boolean er_doed;
    ZonedDateTime doed_fra_dato;
    ZonedDateTime tidsstempel;
}
