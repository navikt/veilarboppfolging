package no.nav.veilarboppfolging.client.digdir_krr;

import lombok.*;

@AllArgsConstructor
@With
@NoArgsConstructor
@Getter
@Data
public class KRRData {
    boolean aktiv;
    String personident;
    boolean kanVarsles;
    boolean reservert;
}
