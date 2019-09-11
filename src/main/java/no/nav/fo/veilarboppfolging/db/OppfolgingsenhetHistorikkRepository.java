package no.nav.fo.veilarboppfolging.db;
import no.nav.fo.veilarboppfolging.domain.OppfolgingsenhetEndringData;
import no.nav.sbl.sql.DbConstants;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.order.OrderClause;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static no.nav.sbl.jdbc.Database.hentDato;

@Component
public class OppfolgingsenhetHistorikkRepository {
    private final JdbcTemplate jdbc;

    @Inject
    public OppfolgingsenhetHistorikkRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insertOppfolgingsenhetEndringForAktorId(String aktorId, String enhet) {
        SqlUtils.insert(jdbc, "OPPFOLGINGSENHET_ENDRET")
                .value("aktor_id", aktorId)
                .value("enhet", enhet)
                .value("endret_dato", DbConstants.CURRENT_TIMESTAMP)
                .execute();
    }

    public List<OppfolgingsenhetEndringData> hentOppfolgingsenhetEndringerForAktorId(String aktorId) {
        return SqlUtils.select(jdbc, "OPPFOLGINGSENHET_ENDRET", OppfolgingsenhetHistorikkRepository::mapper)
                .column("enhet")
                .column("endret_dato")
                .where(WhereClause.equals("aktor_id", aktorId))
                .orderBy(OrderClause.desc("endret_dato"))
                .executeToList();
    }

    private static OppfolgingsenhetEndringData mapper(ResultSet resultset) throws SQLException {
        return OppfolgingsenhetEndringData.builder()
                .enhet(resultset.getString("enhet"))
                .endretDato(hentDato(resultset, "endret_dato"))
                .build();
    }
}
