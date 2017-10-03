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
        return db.queryForList("SELECT VEILEDER " +
                        "FROM SITUASJON " +
                        "WHERE AKTORID = ? ",
                aktorId)
                .stream()
                .findFirst()
                .map(rad -> (String) rad.get("VEILEDER"))
                .orElse(null);
    }

    @Transactional
    public void upsertVeilederTilordning(String aktoerId, String veileder) {
        val rowsUpdated = db.update(
                "INSERT INTO SITUASJON(AKTORID, VEILEDER, OPPFOLGING, OPPDATERT) " +
                        "SELECT ?, ?, 0, CURRENT_TIMESTAMP FROM DUAL " +
                        "WHERE NOT EXISTS(SELECT * FROM SITUASJON WHERE AKTORID=?)",
                aktoerId, veileder, aktoerId);

        if (rowsUpdated == 0) {
            db.update(
                    "UPDATE SITUASJON SET VEILEDER = ?, OPPDATERT=CURRENT_TIMESTAMP WHERE AKTORID = ?",
                    veileder,
                    aktoerId);
        }
        oppfolgingRepository.startOppfolgingHvisIkkeAlleredeStartet(aktoerId);

    }
}
