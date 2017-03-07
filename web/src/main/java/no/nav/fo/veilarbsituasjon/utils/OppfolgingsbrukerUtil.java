package no.nav.fo.veilarbsituasjon.utils;


import no.nav.fo.veilarbsituasjon.domain.OppfolgingBruker;

import java.sql.ResultSet;
import java.sql.SQLException;

public class OppfolgingsbrukerUtil {

    public static OppfolgingBruker mapRadTilOppfolgingsbruker(ResultSet rs) {
        try {
            return new OppfolgingBruker()
                    .setAktoerid(rs.getString("AKTOERID"))
                    .setVeileder(rs.getString("VEILEDER"))
                    .setEndretTimestamp(rs.getTimestamp("OPPDATERT"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
