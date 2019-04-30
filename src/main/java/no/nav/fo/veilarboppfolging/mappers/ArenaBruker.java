package no.nav.fo.veilarboppfolging.mappers;

import lombok.*;

import java.time.ZonedDateTime;

@Data
public class ArenaBruker {
    public String aktoerid;
    public String fodselsnr;
    public String formidlingsgruppekode;
    public String kvalifiseringsgruppekode;
    public ZonedDateTime iserv_fra_dato;
    public String hovedmaalkode;
    public String nav_kontor;
}
