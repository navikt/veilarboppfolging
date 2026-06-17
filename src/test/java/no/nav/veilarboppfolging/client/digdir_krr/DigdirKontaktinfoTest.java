package no.nav.veilarboppfolging.client.digdir_krr;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DigdirKontaktinfoTest {

    private DigdirKontaktinfo kontaktinfo(boolean aktiv, boolean kanVarsles, boolean reservert, String personident) {
        return new DigdirKontaktinfo(
                personident, aktiv, kanVarsles, reservert,
                null, null, null, null, null, null, null, null
        );
    }

    @Test
    public void kan_ikke_varsles_men_aktiv_settes_riktig() {
        var kanVarsles = false;
        var reservert = false;
        assertEquals(kontaktinfo(true, kanVarsles, reservert, "1234567890")
        .toKrrData(), new KRRData(
            true,
            "1234567890",
                kanVarsles,
                reservert
        ));
    }

    @Test
    public void skal_sette_reserver_hvis_reservert() {
        var kanVarsles = true;
        var reservert = false;
        assertEquals(kontaktinfo(true, kanVarsles, reservert, "1234567890")
                .toKrrData(), new KRRData(
                true,
                "1234567890",
                kanVarsles,
                reservert
        ));
    }

    @Test
    public void ikke_aktiv_skal_vere_ikke_aktiv() {
        var kanVarsles = false;
        var reservert = false;
        assertEquals(kontaktinfo(false, kanVarsles, reservert, "1234567890")
                .toKrrData(), new KRRData(
                false,
                "1234567890",
                kanVarsles,
                reservert
        ));
    }
}