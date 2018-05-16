package no.nav.fo.veilarboppfolging.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AktiverArbeidssokerData {
    Fnr fnr;
    String kvalifiseringsgruppekode;
}
