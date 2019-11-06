package no.nav.fo.veilarboppfolging.mappers;

import lombok.*;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
public class VeilarbArenaOppfolging {
    public String aktoerid;
    public String fodselsnr;
    public String formidlingsgruppekode;
    public String kvalifiseringsgruppekode;
    public String rettighetsgruppekode;
    public ZonedDateTime iserv_fra_dato;
    public String hovedmaalkode;
    public String nav_kontor;
}
