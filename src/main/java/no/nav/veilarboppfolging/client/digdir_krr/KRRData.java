package no.nav.veilarboppfolging.client.digdir_krr;

import lombok.*;

@AllArgsConstructor
@With
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class KRRData {
    String personident;
    boolean kanVarsles;
    boolean reservert;
    String epostadresse;
    String mobiltelefonnummer;
}
