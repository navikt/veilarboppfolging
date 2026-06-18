package no.nav.veilarboppfolging.client.digdir_krr;



public record DigdirKontaktinfo(
     String personident,
     boolean aktiv,
     boolean kanVarsles,
     boolean reservert,
     String spraak,
     String spraakOppdatert,
     String epostadresse,
     String epostadresseOppdatert,
     String epostadresseVerifisert,
     String mobiltelefonnummer,
     String mobiltelefonnummerOppdatert,
     String mobiltelefonnummerVerifisert
) {
   public KRRData toKrrData() {
      return new KRRData(
           aktiv,
           personident,
           kanVarsles,
           reservert
      );
   }
}
