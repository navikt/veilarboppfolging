package no.nav.fo.veilarbsituasjon.utils;


import no.nav.fo.veilarbsituasjon.rest.domain.OppfolgingBruker;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;

public class OppfolgingsbrukerUtil {

    public static OppfolgingBruker mapRadTilOppfolgingsbruker(Map<String, Object> rad) {
        return new OppfolgingBruker()
                .setAktoerid((String) rad.get("AKTORID"))
                .setVeileder((String) rad.get("VEILEDER"))
                .setOppfolging(rad.get("OPPFOLGING").equals(BigDecimal.ONE))
                .setEndretTimestamp((Timestamp) (rad.get("OPPDATERT")));
    }

}
