package no.nav.fo.veilarboppfolging.utils;
import no.nav.fo.veilarboppfolging.domain.BrukerRegistrering;
import no.nav.fo.veilarboppfolging.domain.StartRegistreringStatus;
import org.junit.jupiter.api.Test;

import static no.nav.fo.veilarboppfolging.services.registrerBruker.Konstanter.*;
import static no.nav.fo.veilarboppfolging.utils.SelvgaaendeUtil.NUS_KODE_2;
import static no.nav.fo.veilarboppfolging.utils.SelvgaaendeUtil.erBesvarelseneValidertSomSelvgaaende;
import static no.nav.fo.veilarboppfolging.utils.SelvgaaendeUtil.erSelvgaaende;
import static org.assertj.core.api.Java6Assertions.assertThat;


public class SelvgaaendeUtilsTest {

    @Test
    void skalValidereSelvgaaendeUnderoppfolging() {
        BrukerRegistrering bruker = getBrukerBesvarelse();
        StartRegistreringStatus startRegistreringStatus = new StartRegistreringStatus()
                .setUnderOppfolging(true)
                .setOppfyllerKravForAutomatiskRegistrering(true);
        assertThat(erSelvgaaende(bruker, startRegistreringStatus )).isFalse();
    }

    @Test
    void skalValidereSelvgaaendeOppfyllerkrav() {
        BrukerRegistrering bruker = getBrukerBesvarelse();
        StartRegistreringStatus startRegistreringStatus = new StartRegistreringStatus()
                .setUnderOppfolging(false)
                .setOppfyllerKravForAutomatiskRegistrering(false);
        assertThat(erSelvgaaende(bruker, startRegistreringStatus )).isFalse();
    }

    @Test
    void brukerMedBesvarelseNus_Kode_0_SkalFeile() {
        BrukerRegistrering bruker = BrukerRegistrering.builder()
                .nusKode(NUS_KODE_0)
                .yrkesPraksis(null)
                .opprettetDato(null)
                .enigIOppsummering(ENIG_I_OPPSUMMERING)
                .oppsummering(OPPSUMMERING)
                .harHelseutfordringer(HAR_INGEN_HELSEUTFORDRINGER)
                .build();
        assertThat(erBesvarelseneValidertSomSelvgaaende(bruker)).isFalse();
    }

    @Test
    void brukerMedBesvarelseNus_Kode_2_SkalIkkeFeile() {
        BrukerRegistrering bruker = BrukerRegistrering.builder()
                .nusKode(NUS_KODE_2)
                .yrkesPraksis(null)
                .opprettetDato(null)
                .enigIOppsummering(ENIG_I_OPPSUMMERING)
                .oppsummering(OPPSUMMERING)
                .harHelseutfordringer(HAR_INGEN_HELSEUTFORDRINGER)
                .build();
        assertThat(erBesvarelseneValidertSomSelvgaaende(bruker)).isTrue();
    }

    private BrukerRegistrering getBrukerBesvarelse() {
        return  BrukerRegistrering.builder()
                .nusKode(NUS_KODE_4)
                .yrkesPraksis(null)
                .opprettetDato(null)
                .enigIOppsummering(ENIG_I_OPPSUMMERING)
                .oppsummering(OPPSUMMERING)
                .harHelseutfordringer(HAR_INGEN_HELSEUTFORDRINGER)
                .build();
    }

}