package no.nav.veilarboppfolging.repository.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;

@Data
@Accessors(chain = true)
public class VeilederTilordningEntity {
    String aktorId;
    String veilederId;
    boolean oppfolging;
    boolean nyForVeileder;
    ZonedDateTime sistTilordnet;
    ZonedDateTime sistOppdatert;
}
