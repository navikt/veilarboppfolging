package no.nav.veilarboppfolging.repository;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.veilarboppfolging.domain.IservMapper;
import no.nav.veilarboppfolging.domain.VeilarbArenaOppfolgingEndret;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static no.nav.veilarboppfolging.utils.DbUtils.firstOrNull;

@Slf4j
@Repository
public class UtmeldingRepository {

    private final JdbcTemplate db;

    @Autowired
    public UtmeldingRepository(JdbcTemplate db) {
        this.db = db;
    }

    public IservMapper eksisterendeIservBruker(VeilarbArenaOppfolgingEndret oppfolgingEndret) {
        String sql = "SELECT * FROM UTMELDING WHERE aktor_id = ?";
        return firstOrNull(db.query(sql, UtmeldingRepository::mapper, oppfolgingEndret.getAktoerid()));
    }

    @SneakyThrows
    public void updateUtmeldingTabell(VeilarbArenaOppfolgingEndret oppfolgingEndret) {
        String sql = "UPDATE UTMELDING SET iserv_fra_dato = ?, oppdatert_dato = CURRENT_TIMESTAMP WHERE aktor_id = ?";
        Timestamp nyIservFraDato = Timestamp.from(oppfolgingEndret.getIserv_fra_dato().toInstant());

        db.update(sql, nyIservFraDato, oppfolgingEndret.getAktoerid());

        log.info("ISERV bruker med aktorid {} har blitt oppdatert inn i UTMELDING tabell", oppfolgingEndret.getAktoerid());
    }

    public void insertUtmeldingTabell(VeilarbArenaOppfolgingEndret oppfolgingEndret) {
        Timestamp iservFraDato = Timestamp.from(oppfolgingEndret.getIserv_fra_dato().toInstant());

        String sql = "INSERT INTO UTMELDING (aktor_id, iserv_fra_dato, oppdatert_dato) VALUES (?, ?, CURRENT_TIMESTAMP)";

        db.update(sql, oppfolgingEndret.getAktoerid(), iservFraDato);

        log.info("ISERV bruker med aktorid {} og iserv_fra_dato {} har blitt insertert inn i UTMELDING tabell",
                oppfolgingEndret.getAktoerid(),
                iservFraDato
        );
    }

    public void slettBrukerFraUtmeldingTabell(String aktoerId) {
        String sql = "DELETE FROM UTMELDING WHERE aktor_id = ?";
        
        int rowsDeleted = db.update(sql, aktoerId);
        
        if (rowsDeleted > 0) {
            log.info("Aktorid {} har blitt slettet fra UTMELDING tabell", aktoerId);
        }
    }

    public List<IservMapper> finnBrukereMedIservI28Dager() {
        Timestamp tilbake28 = Timestamp.valueOf(LocalDateTime.now().minusDays(28));
        String sql = "SELECT * FROM UTMELDING WHERE aktor_id IS NOT NULL AND iserv_fra_dato < ?";
        return db.query(sql, UtmeldingRepository::mapper, tilbake28);
    }

    private static IservMapper mapper(ResultSet resultSet, int row) throws SQLException {
        return new IservMapper(
                resultSet.getString("aktor_id"),
                resultSet.getTimestamp("iserv_fra_dato").toLocalDateTime().atZone(ZoneId.systemDefault())
        );
    }

}
