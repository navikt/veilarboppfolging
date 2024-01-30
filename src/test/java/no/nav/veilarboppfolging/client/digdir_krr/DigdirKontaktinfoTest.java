package no.nav.veilarboppfolging.client.digdir_krr;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DigdirKontaktinfoTest {

    @Test
    public void kan_ikke_varsles_men_aktiv_settes_riktig() {
        var kanVarsles = false;
        var reservert = false;
        assertEquals(new DigdirKontaktinfo()
            .setAktiv(true)
            .setReservert(reservert)
            .setKanVarsles(kanVarsles)
            .setPersonident("1234567890")
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
        assertEquals(new DigdirKontaktinfo()
                .setAktiv(true)
                .setReservert(reservert)
                .setKanVarsles(kanVarsles)
                .setPersonident("1234567890")
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
        assertEquals(new DigdirKontaktinfo()
                .setAktiv(false)
                .setReservert(reservert)
                .setKanVarsles(kanVarsles)
                .setPersonident("1234567890")
                .toKrrData(), new KRRData(
                false,
                "1234567890",
                kanVarsles,
                reservert
        ));
    }
}