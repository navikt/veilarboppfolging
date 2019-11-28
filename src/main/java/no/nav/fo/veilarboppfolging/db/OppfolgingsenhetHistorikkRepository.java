package no.nav.fo.veilarboppfolging.db;

import no.nav.fo.veilarboppfolging.domain.OppfolgingsenhetEndringData;
import no.nav.internal.OppfolgingEnhetDTO;
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

    private static final String TABLENAME = "OPPFOLGINGSENHET_ENDRET";

    public void insertOppfolgingsenhetEndring(OppfolgingEnhetDTO dto) {
        SqlUtils.insert(jdbc, TABLENAME)
                .value("aktor_id", dto.getAktorId())
                .value("enhet", dto.getEnhetId())
                .value("endret_dato", DbConstants.CURRENT_TIMESTAMP)
                .value("enhet_seq", DbConstants.nextSeq("ENHET_SEQ"))
                .execute();
    }

    public void insertOppfolgingsenhetEndring(List<OppfolgingEnhetDTO> dtoer) {
        SqlUtils.updateBatch(jdbc, TABLENAME, OppfolgingEnhetDTO.class)
                .add("aktor_id", OppfolgingEnhetDTO::getAktorId, String.class)
                .add("enhet", OppfolgingEnhetDTO::getAktorId, String.class)
                .add("endret_dato", DbConstants.CURRENT_TIMESTAMP)
                .add("enhet_seq", DbConstants.nextSeq("ENHET_SEQ"))
                .execute(dtoer);
    }

    public void insertOppfolgingsenhetEndringForAktorId(String aktorId, String enhet) {
        SqlUtils.insert(jdbc, TABLENAME)
                .value("aktor_id", aktorId)
                .value("enhet", enhet)
                .value("endret_dato", DbConstants.CURRENT_TIMESTAMP)
                .value("enhet_seq", DbConstants.nextSeq("ENHET_SEQ"))
                .execute();
    }

    public List<OppfolgingsenhetEndringData> hentOppfolgingsenhetEndringerForAktorId(String aktorId) {
        return SqlUtils.select(jdbc, TABLENAME, OppfolgingsenhetHistorikkRepository::mapper)
                .column("enhet")
                .column("endret_dato")
                .where(WhereClause.equals("aktor_id", aktorId))
                .orderBy(OrderClause.desc("enhet_seq"))
                .executeToList();
    }

    private static OppfolgingsenhetEndringData mapper(ResultSet resultset) throws SQLException {
        return OppfolgingsenhetEndringData.builder()
                .enhet(resultset.getString("enhet"))
                .endretDato(hentDato(resultset, "endret_dato"))
                .build();
    }
}
