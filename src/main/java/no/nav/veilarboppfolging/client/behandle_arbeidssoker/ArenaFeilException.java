package no.nav.veilarboppfolging.client.behandle_arbeidssoker;

public class ArenaFeilException extends RuntimeException {
    public final Type type;

    public ArenaFeilException(Type type) {
        this.type = type;
    }

    public enum Type {
        BRUKER_ER_UKJENT,
        BRUKER_KAN_IKKE_REAKTIVERES,
        BRUKER_ER_DOD_UTVANDRET_ELLER_FORSVUNNET,
        BRUKER_MANGLER_ARBEIDSTILLATELSE,
        BRUKER_KAN_IKKE_REAKTIVERES_FORENKLET
    }
}

