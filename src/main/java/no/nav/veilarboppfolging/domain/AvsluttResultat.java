package no.nav.veilarboppfolging.domain;


public record AvsluttResultat(
    int antallAvsluttet,
    int antallKunneIkkeAvsluttes
) {}