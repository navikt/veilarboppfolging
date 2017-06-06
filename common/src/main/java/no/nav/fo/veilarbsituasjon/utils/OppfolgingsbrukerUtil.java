package no.nav.fo.veilarbsituasjon.utils;


import no.nav.fo.veilarbsituasjon.domain.OppfolgingBruker;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;

public class OppfolgingsbrukerUtil {

    public static OppfolgingBruker mapRadTilOppfolgingsbruker(ResultSet rs) {
        try {
            return new OppfolgingBruker()
                    .setAktoerid(rs.getString("AKTOERID"))
                    .setVeileder(rs.getString("VEILEDER"))
                    .setOppfolging(rs.getBoolean("OPPFOLGING"))
                    .setEndretTimestamp(rs.getTimestamp("OPPDATERT"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static OppfolgingBruker mapRadTilOppfolgingsbruker(Map<String, Object> rad) {
        return new OppfolgingBruker()
                .setAktoerid((String) rad.get("AKTOERID"))
                .setVeileder((String) rad.get("VEILEDER"))
                .setOppfolging(rad.get("OPPFOLGING").equals(BigDecimal.ONE))
                .setEndretTimestamp((Timestamp) (rad.get("OPPDATERT")));
    }

}
