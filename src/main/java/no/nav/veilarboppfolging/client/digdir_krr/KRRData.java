package no.nav.veilarboppfolging.client.digdir_krr;

import lombok.*;

@AllArgsConstructor
@With
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class KRRData {
    Boolean aktiv;
    String personident;
    boolean kanVarsles;
    boolean reservert;
    String epostadresse;
    String mobiltelefonnummer;
}
