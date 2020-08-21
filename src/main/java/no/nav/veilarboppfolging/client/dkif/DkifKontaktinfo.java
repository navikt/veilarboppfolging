package no.nav.veilarboppfolging.client.dkif;

import lombok.Data;

@Data
public class DkifKontaktinfo {
    String personident;
    boolean kanVarsles;
    boolean reservert;
    String epostadresse;
    String mobiltelefonnummer;
}