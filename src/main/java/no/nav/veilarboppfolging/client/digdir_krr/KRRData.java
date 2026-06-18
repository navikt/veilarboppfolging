package no.nav.veilarboppfolging.client.digdir_krr;

public record KRRData(
    boolean aktiv,
    String personident,
    boolean kanVarsles,
    boolean reservert
) {}
