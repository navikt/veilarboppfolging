package no.nav.fo.veilarboppfolging.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Tilordning {
    String aktorId;
    String veilederId;
    boolean oppfolging;
    String sistOppdatert;
}
