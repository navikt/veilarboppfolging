package no.nav.fo.veilarbsituasjon.config;

import no.nav.sbl.featuretoggle.SystemPropertyFeatureToggle;

public enum OppfolgingFeature implements SystemPropertyFeatureToggle {
    SKIP_VALIDERING_DIFI("feature.skip.validering.difi", true); // TODO denne bør fjernes eller sette default=false før prodsetting

    private final String systemVariabel;
    private final boolean defaultAktiv;

    OppfolgingFeature(String systemVariabel, boolean defaultAktiv) {
        this.systemVariabel = systemVariabel;
        this.defaultAktiv = defaultAktiv;
    }

    @Override
    public String getSystemVariabelNavn() {
        return systemVariabel;
    }

    @Override
    public boolean erDefaultAktiv() {
        return defaultAktiv;
    }

}
