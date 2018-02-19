package no.nav.fo.veilarboppfolging.utils;
import no.nav.fo.veilarboppfolging.domain.BrukerRegistrering;
import org.junit.jupiter.api.Test;

import static no.nav.fo.veilarboppfolging.services.registrerBruker.Konstanter.*;
import static no.nav.fo.veilarboppfolging.utils.SelvgaaendeUtil.erBesvarelseneValidertSomSelvgaaende;
import static org.assertj.core.api.Java6Assertions.assertThat;


public class SelvgaaendeUtilsTest {

    @Test
    void brukerHarIkkeBestattUtdanning() {
        BrukerRegistrering bruker = new BrukerRegistrering(
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

}