package no.nav.veilarboppfolging.client.digdir_krr;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DigdirKontaktinfo {
   String personident;
   boolean aktiv;
   boolean kanVarsles;
   boolean reservert;
   String spraak;
   String spraakOppdatert;
   String epostadresse;
   String epostadresseOppdatert;
   String epostadresseVerifisert;
   String mobiltelefonnummer;
   String mobiltelefonnummerOppdatert;
   String mobiltelefonnummerVerifisert;
}
