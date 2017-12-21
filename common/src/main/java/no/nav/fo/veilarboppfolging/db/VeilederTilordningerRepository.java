package no.nav.fo.veilarboppfolging.db;


import lombok.val;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

public class VeilederTilordningerRepository {

    private JdbcTemplate db;
    private OppfolgingRepository oppfolgingRepository;

    public VeilederTilordningerRepository(JdbcTemplate db, OppfolgingRepository oppfolgingRepository) {
        this.db = db;
        this.oppfolgingRepository = oppfolgingRepository;
    }

    public String hentTilordningForAktoer(String aktorId) {
        return db.queryForList("SELECT veileder " +
                        "FROM OPPFOLGINGSTATUS " +
                        "WHERE aktor_id = ? ",
                aktorId)
                .stream()
                .findFirst()
                .map(rad -> (String) rad.get("veileder"))
                .orElse(null);
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
