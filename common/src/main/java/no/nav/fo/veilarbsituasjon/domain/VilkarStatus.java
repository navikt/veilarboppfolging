package no.nav.fo.veilarbsituasjon.domain;

import no.nav.tjeneste.virksomhet.behandlesituasjon.v1.informasjon.Vilkaarsstatuser;

public enum VilkarStatus {
    AVSLATT,
    GODKJENT,
    IKKE_BESVART;

    public static VilkarStatus mapWsTilVilkarStatus(Vilkaarsstatuser vilkaarStatuser) {
        switch (vilkaarStatuser) {
            case AVSLAATT:
                return AVSLATT;
            case GODKJENT:
                return GODKJENT;
            default:
                return IKKE_BESVART;
        }
    }
}
