package no.nav.fo.veilarboppfolging.domain;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AktiverArbeidssokerData {
    private Fnr fnr;
    private String kvalifiseringsgruppekode;
}
