package no.nav.fo.veilarboppfolging.db;


import lombok.val;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingBruker;
import no.nav.fo.veilarboppfolging.utils.OppfolgingsbrukerUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class BrukerRepository {

    private JdbcTemplate db;
    private SituasjonRepository situasjonRepository;

    public BrukerRepository(JdbcTemplate db, SituasjonRepository situasjonRepository) {
        this.db = db;
        this.situasjonRepository = situasjonRepository;
    }

    public List<OppfolgingBruker> hentTilordningerEtterTimestamp(Timestamp timestamp) {
        return db
                .queryForList(hentVeilederTilordningerEtterTimestampSQL(), timestamp)
                .stream()
                .map(OppfolgingsbrukerUtil::mapRadTilOppfolgingsbruker)
                .collect(toList());
    }

    public OppfolgingBruker hentTilordningForAktoer(String aktorId) {
        return db.queryForList(hentTilordningForAktoer(), aktorId)
                .stream()
                .findFirst()
                .map(OppfolgingsbrukerUtil::mapRadTilOppfolgingsbruker)
                .orElse(OppfolgingBruker
                        .builder()
                        .aktoerid(aktorId)
                        .build()
                );
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
        situasjonRepository.startOppfolgingHvisIkkeAlleredeStartet(aktoerId);
        
    }

    private String hentTilordninger() {
        return "SELECT AKTORID, VEILEDER, OPPFOLGING, OPPDATERT " +
                "FROM SITUASJON";
    }

    private String hentTilordningForAktoer() {
        return hentTilordninger() +
                " WHERE AKTORID = ?";
    }

    private String hentVeilederTilordningerEtterTimestampSQL() {
        return hentTilordninger() +
                " WHERE OPPDATERT >= ?";
    }
}
