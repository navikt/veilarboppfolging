package no.nav.fo.veilarbsituasjon.rest;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PortefoljeRessursTest {

    @Test
    public void skalTildeleVeileder() throws Exception {
        String fraVeileder = "AAAAAAA";
        String tilVeileder = "BBBBBBB";
        String eksisterendeVeileder = "AAAAAAA";
        boolean result = PortefoljeRessurs.kanSetteNyVeileder(fraVeileder, tilVeileder, eksisterendeVeileder);
        assertTrue(result);
    }

    @Test
    public void skalTildeleVeilederOmEksisterendeErNull() throws Exception {
        String fraVeileder = "AAAAAAA";
        String tilVeileder = "BBBBBBB";
        String eksisterendeVeileder = null;
        boolean result = PortefoljeRessurs.kanSetteNyVeileder(fraVeileder, tilVeileder, eksisterendeVeileder);
        assertTrue(result);
    }

    @Test
    public void skalIkkeTildeleVeilederOmEksisterendeErUlikFraVeileder() throws Exception {
        String fraVeileder = "AAAAAAA";
        String tilVeileder = "BBBBBBB";
        String eksisterendeVeileder = "CCCCCC";
        boolean result = PortefoljeRessurs.kanSetteNyVeileder(fraVeileder, tilVeileder, eksisterendeVeileder);
        assertFalse(result);
    }

    @Test
    public void skalIkkeTildeleVeilederOmTilVeilederErNull() throws Exception {
        String fraVeileder = "AAAAAAA";
        String tilVeileder = null;
        String eksisterendeVeileder = "CCCCCC";
        boolean result = PortefoljeRessurs.kanSetteNyVeileder(fraVeileder, tilVeileder, eksisterendeVeileder);
        assertFalse(result);
    }
}