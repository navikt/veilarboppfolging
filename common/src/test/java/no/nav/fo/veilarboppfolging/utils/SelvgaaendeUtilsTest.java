package no.nav.fo.veilarboppfolging.utils;
import no.nav.fo.veilarboppfolging.domain.BrukerRegistrering;
import org.junit.jupiter.api.Test;

import static no.nav.fo.veilarboppfolging.services.registrerBruker.Konstanter.*;
import static no.nav.fo.veilarboppfolging.utils.SelvgaaendeUtil.erBesvarelseneValidertSomSelvgaaende;
import static org.assertj.core.api.Java6Assertions.assertThat;


public class SelvgaaendeUtilsTest {

    @Test
    void brukerHarIkkeBestattUtdanning() {
        BrukerRegistrering bruker = BrukerRegistrering.builder()
                .nusKode(NUS_KODE_4)
                .yrkesPraksis(null)
                .opprettetDato(null)
                .enigIOppsummering(ENIG_I_OPPSUMMERING)
                .oppsummering(OPPSUMMERING)
                .utdanningBestatt(UTDANNING_IKKE_BESTATT)
                .utdanningGodkjentNorge(UTDANNING_GODKJENT_NORGE)
                .harHelseutfordringer(HAR_INGEN_HELSEUTFORDRINGER)
                .situasjon(MISTET_JOBBEN)
                .build();
        assertThat(!erBesvarelseneValidertSomSelvgaaende(bruker)).isTrue();
    }

    @Test
    void brukerHarIkkeGodkjentUtdanning() {
        BrukerRegistrering bruker = BrukerRegistrering.builder()
                .nusKode(NUS_KODE_4)
                .yrkesPraksis(null)
                .opprettetDato(null)
                .enigIOppsummering(ENIG_I_OPPSUMMERING)
                .oppsummering(OPPSUMMERING)
                .utdanningBestatt(UTDANNING_BESTATT)
                .utdanningGodkjentNorge(UTDANNING_IKKE_GODKJENT_NORGE)
                .harHelseutfordringer(HAR_INGEN_HELSEUTFORDRINGER)
                .situasjon(MISTET_JOBBEN)
                .build();
        assertThat(!erBesvarelseneValidertSomSelvgaaende(bruker)).isTrue();
    }

    @Test
    void brukerHarGrunnskole() {
        BrukerRegistrering bruker = BrukerRegistrering.builder()
                .nusKode(NUS_KODE_0)
                .yrkesPraksis(null)
                .opprettetDato(null)
                .enigIOppsummering(ENIG_I_OPPSUMMERING)
                .oppsummering(OPPSUMMERING)
                .utdanningBestatt(UTDANNING_BESTATT)
                .utdanningGodkjentNorge(UTDANNING_GODKJENT_NORGE)
                .harHelseutfordringer(HAR_INGEN_HELSEUTFORDRINGER)
                .situasjon(MISTET_JOBBEN)
                .build();
        assertThat(!erBesvarelseneValidertSomSelvgaaende(bruker)).isTrue();
    }

}