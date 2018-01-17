package no.nav.fo.veilarboppfolging.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class Tilordning {
    String aktorId;
    String veilederId;
    boolean oppfolging;
    boolean nyForVeileder;
    Date sistTilordnet;
    Date sistOppdatert;
}
