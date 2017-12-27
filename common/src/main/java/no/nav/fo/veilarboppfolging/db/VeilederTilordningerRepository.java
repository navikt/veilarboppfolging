package no.nav.fo.veilarboppfolging.db;


import lombok.SneakyThrows;
import lombok.val;
import no.nav.sbl.jdbc.Database;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;

public class VeilederTilordningerRepository {

    private Database db;
    private OppfolgingRepository oppfolgingRepository;

    public VeilederTilordningerRepository(Database db, OppfolgingRepository oppfolgingRepository) {
        this.db = db;
        this.oppfolgingRepository = oppfolgingRepository;
    }

    public String hentTilordningForAktoer(String aktorId) {
        return db.query("SELECT veileder " +
                        "FROM OPPFOLGINGSTATUS " +
                        "WHERE aktor_id = ? ",this::map,
                aktorId)
                .stream()
                .findFirst()
                .orElse(null);
    }

    @SneakyThrows
    private String map(ResultSet resultSet) {
        return resultSet.getString("veileder");
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
}
