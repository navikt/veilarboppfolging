package no.nav.fo.veilarboppfolging.utils;


import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingBruker;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;

public class OppfolgingsbrukerUtil {

    public static OppfolgingBruker mapRadTilOppfolgingsbruker(Map<String, Object> rad) {
        return OppfolgingBruker
                .builder()
                .aktoerid((String) rad.get("AKTORID"))
                .veileder((String) rad.get("VEILEDER"))
                .oppfolging(rad.get("OPPFOLGING").equals(BigDecimal.ONE))
                .endretTimestamp((Timestamp) (rad.get("OPPDATERT")))
                .build();
    }

}
