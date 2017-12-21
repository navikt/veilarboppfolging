package no.nav.fo.veilarboppfolging.db;


import lombok.SneakyThrows;
import lombok.val;
import no.nav.sbl.sql.SqlUtils;
import no.nav.sbl.sql.where.WhereClause;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.sql.ResultSet;

public class VeilederTilordningerRepository {

    private JdbcTemplate db;
    private OppfolgingRepository oppfolgingRepository;

    public VeilederTilordningerRepository(JdbcTemplate db, OppfolgingRepository oppfolgingRepository) {
        this.db = db;
        this.oppfolgingRepository = oppfolgingRepository;
    }

    public String hentTilordningForAktoer(String aktorId) {

        return SqlUtils.select(db.getDataSource(), OppfolgingsStatusRepository.TABLE_NAME, this::getVeileder)
                .column("veileder")
                .where(WhereClause.equals("aktor_id", aktorId))
                .execute();
    }

    @Transactional
    public void upsertVeilederTilordning(String aktoerId, String veileder) {
        val rowsUpdated = db.update(
                "INSERT INTO OPPFOLGINGSTATUS(aktor_id, veileder, under_oppfolging, oppdatert) " +
                        "SELECT ?, ?, 0, CURRENT_TIMESTAMP FROM DUAL " +
                        "WHERE NOT EXISTS(SELECT * FROM OPPFOLGINGSTATUS WHERE aktor_id=?)",
                aktoerId, veileder, aktoerId);

        if (rowsUpdated == 0) {
            db.update(
                    "UPDATE OPPFOLGINGSTATUS SET veileder = ?, oppdatert=CURRENT_TIMESTAMP WHERE aktor_id = ?",
                    veileder,
                    aktoerId);
        }
        oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(aktoerId);

    }

    @SneakyThrows
    private String getVeileder(ResultSet resultSet) {
        return resultSet.getString("veileder");
    }
}
