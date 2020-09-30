package no.nav.veilarboppfolging.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;
import java.util.Date;

@Data
@Accessors(chain = true)
public class Tilordning {
    String aktorId;
    String veilederId;
    boolean oppfolging;
    boolean nyForVeileder;
    ZonedDateTime sistTilordnet;
    ZonedDateTime sistOppdatert;
}
