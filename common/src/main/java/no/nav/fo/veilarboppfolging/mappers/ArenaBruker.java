package no.nav.fo.veilarboppfolging.mappers;

import lombok.*;

import java.time.ZonedDateTime;


@Getter
@Setter
public class ArenaBruker {

    public String aktoerid;
    public String formidlingsgruppekode;
    public String iserv_fra_dato;
    public String fodselsnr;
    public String etternavn;
    public String fornavn;
    public String nav_kontor;
    public String kvalifiseringsgruppekode;
    public String rettighetsgruppekode;
    public String hovedmaalkode;
    public String sikkerhetstiltak_type_kode;
    public String fr_kode;
    public Boolean har_oppfolgingssak;
    public String sperret_ansatt;
    public Boolean er_doed;
    public ZonedDateTime doed_fra_dato;
    public ZonedDateTime tidsstempel;
}
