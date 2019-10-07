package no.nav.fo.veilarboppfolging.utils;


import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingFeedDTO;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

public class OppfolgingFeedUtil {

    public static OppfolgingFeedDTO mapRadTilOppfolgingFeedDTO(ResultSet rad) throws SQLException {
        return OppfolgingFeedDTO
                .builder()
                .aktoerid((String) rad.get("AKTOR_ID"))
                .veileder((String) rad.get("VEILEDER"))
                .oppfolging(rad.get("UNDER_OPPFOLGING").equals(BigDecimal.ONE))
                .nyForVeileder(rad.get("NY_FOR_VEILEDER").equals(BigDecimal.ONE))
                .endretTimestamp((Timestamp) (rad.get("OPPDATERT")))
                .startDato((Timestamp) (rad.get("STARTDATO")))
                .feedId((BigDecimal) rad.get("FEED_ID"))
                .manuell(BigDecimal.ONE.equals(rad.get("MANUELL")))
                .build();
    }
}
