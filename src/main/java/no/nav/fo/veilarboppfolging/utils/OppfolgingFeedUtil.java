package no.nav.fo.veilarboppfolging.utils;


import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingFeedDTO;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

public class OppfolgingFeedUtil {

    public static OppfolgingFeedDTO mapRadTilOppfolgingFeedDTO(ResultSet rad) throws SQLException {
        return OppfolgingFeedDTO
                .builder()
                .aktoerid(rad.getString("AKTOR_ID"))
                .veileder(rad.getString("VEILEDER"))
                .oppfolging(rad.getBigDecimal("UNDER_OPPFOLGING").equals(BigDecimal.ONE))
                .nyForVeileder(rad.getBigDecimal("NY_FOR_VEILEDER").equals(BigDecimal.ONE))
                .endretTimestamp(rad.getTimestamp("OPPDATERT"))
                .startDato(rad.getTimestamp("STARTDATO"))
                .feedId(rad.getBigDecimal("FEED_ID"))
                .manuell(BigDecimal.ONE.equals(rad.getBigDecimal("MANUELL")))
                .build();
    }
}
