package no.nav.veilarboppfolging.client.digdir_krr;

public class KRRKontaktInfoV3 {
    String personidentifikator;
    JaEllerNei reservasjon;
    String reservasjonstidspunkt;
    String reservasjon_oppdatert;
    KRRStatus status;
    VarslingsStatus varslingsstatus;
    KontaktInformasjon kontaktinformasjon;
    DigitalPost digital_post;
    String sertifikat;
    String spraak;
    String spraak_oppdatert;

    public KRRData toKrrData() {
        return new KRRData(
            personidentifikator,
            varslingsstatus == VarslingsStatus.KAN_VARSLES,
            reservasjon == JaEllerNei.JA,
            kontaktinformasjon.epostadresse,
            kontaktinformasjon.mobiltelefonnummer
        );
    }
}

class DigitalPost {
    String postkasseadresse;
    String postkasseleverandoeradresse;
}

class KontaktInformasjon {
    String epostadresse;
    String epostadresse_oppdatert;
    String epostadresse_sist_verifisert;
    String epostadresse_sist_validert;
    String mobiltelefonnummer;
    String mobiltelefonnummer_oppdatert;
    String mobiltelefonnummer_sist_verifisert;
    String mobiltelefonnummer_sist_validert;
    String mobiltelefonnummer_valideringstoken;
}

enum VarslingsStatus {
    KAN_IKKE_VARSLES, // Person har ikke utgått kontaktinformasjon
    KAN_VARSLES // Person har utgått kontaktinformasjon, er reservert, er slettet eller finnes ikke i registeret
}

enum KRRStatus {
    AKTIV,
    SLETTET,
    IKKE_REGISTRERT
}

enum JaEllerNei {
    JA,
    NEI
}