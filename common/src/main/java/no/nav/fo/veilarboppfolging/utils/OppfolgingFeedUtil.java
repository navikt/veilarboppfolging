package no.nav.fo.veilarboppfolging.utils;


import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingFeedDTO;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Map;

public class OppfolgingFeedUtil {

    public static OppfolgingFeedDTO mapRadTilOppfolgingFeedDTO(Map<String, Object> rad) {
        return OppfolgingFeedDTO
                .builder()
                .aktoerid((String) rad.get("AKTOR_ID"))
                .veileder((String) rad.get("VEILEDER"))
                .oppfolging(rad.get("UNDER_OPPFOLGING").equals(BigDecimal.ONE))
                .endretTimestamp((Timestamp) (rad.get("OPPDATERT")))
                .build();
    }

}
