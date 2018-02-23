package no.nav.fo.veilarboppfolging.utils;
import no.nav.fo.veilarboppfolging.domain.BrukerRegistrering;
import no.nav.fo.veilarboppfolging.domain.StartRegistreringStatus;
import org.junit.jupiter.api.Test;

import static no.nav.fo.veilarboppfolging.services.registrerBruker.Konstanter.*;
import static no.nav.fo.veilarboppfolging.utils.SelvgaaendeUtil.erBesvarelseneValidertSomSelvgaaende;
import static no.nav.fo.veilarboppfolging.utils.SelvgaaendeUtil.erSelvgaaende;
import static org.assertj.core.api.Java6Assertions.assertThat;


public class SelvgaaendeUtilsTest {

    @Test
    void skalValidereSelvgaaendeBesvarelse() {
        BrukerRegistrering bruker = getBrukerBesvarelseUtdanningIkkeBestatt();
        StartRegistreringStatus startRegistreringStatus = new StartRegistreringStatus()
                .setUnderOppfolging(false)
                .setOppfyllerKravForAutomatiskRegistrering(true);
        assertThat(erSelvgaaende(bruker, startRegistreringStatus )).isFalse();
    }


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
    void brukerHarIkkeBestattUtdanning() {
        BrukerRegistrering bruker = getBrukerBesvarelseUtdanningIkkeBestatt();

        assertThat(!erBesvarelseneValidertSomSelvgaaende(bruker)).isTrue();
    }

    @Test
    void brukerHarIkkeGodkjentUtdanning() {
        BrukerRegistrering bruker = new BrukerRegistrering(
                NUS_KODE_4,
                null,
                null,
                ENIG_I_OPPSUMMERING,
                OPPSUMMERING,
                UTDANNING_BESTATT,
                UTDANNING_IKKE_GODKJENT_NORGE,
                HAR_INGEN_HELSEUTFORDRINGER,
                MISTET_JOBBEN);
        assertThat(!erBesvarelseneValidertSomSelvgaaende(bruker)).isTrue();
    }

    @Test
    void brukerHarGrunnskole() {
        BrukerRegistrering bruker = new BrukerRegistrering(
                NUS_KODE_0,
                null,
                null,
                ENIG_I_OPPSUMMERING,
                OPPSUMMERING,
                UTDANNING_BESTATT,
                UTDANNING_GODKJENT_NORGE,
                HAR_INGEN_HELSEUTFORDRINGER,
                MISTET_JOBBEN);
        assertThat(!erBesvarelseneValidertSomSelvgaaende(bruker)).isTrue();
    }

    private BrukerRegistrering getBrukerBesvarelse() {
        return new BrukerRegistrering(
                    NUS_KODE_4,
                    null,
                    null,
                    ENIG_I_OPPSUMMERING,
                    OPPSUMMERING,
                    UTDANNING_BESTATT,
                    UTDANNING_GODKJENT_NORGE,
                    HAR_INGEN_HELSEUTFORDRINGER,
                    MISTET_JOBBEN
            );
    }

    private BrukerRegistrering getBrukerBesvarelseUtdanningIkkeBestatt() {
        return new BrukerRegistrering(
                NUS_KODE_4,
                null,
                null,
                ENIG_I_OPPSUMMERING,
                OPPSUMMERING,
                UTDANNING_IKKE_BESTATT,
                UTDANNING_GODKJENT_NORGE,
                HAR_INGEN_HELSEUTFORDRINGER,
                MISTET_JOBBEN
        );
    }

}
