package no.nav.veilarboppfolging.repository;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.types.identer.AktorId;
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.InsertIUtmelding;
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.UpdateIservDatoUtmelding;
import no.nav.veilarboppfolging.repository.entity.UtmeldingEntity;
import no.nav.veilarboppfolging.utils.DbUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static no.nav.veilarboppfolging.utils.DbUtils.queryForNullableObject;
import static no.nav.veilarboppfolging.utils.SecureLog.secureLog;

@Slf4j
@Repository
public class UtmeldingRepository {

    private final JdbcTemplate db;

    @Autowired
    public UtmeldingRepository(JdbcTemplate db) {
        this.db = db;
    }

    public Optional<UtmeldingEntity> eksisterendeIservBruker(AktorId aktorId) {
        String sql = "SELECT * FROM UTMELDING WHERE aktor_id = ?";
        return queryForNullableObject(() -> db.queryForObject(sql, UtmeldingRepository::mapper, aktorId.get()));
    }

    @SneakyThrows
    public void updateUtmeldingTabell(UpdateIservDatoUtmelding oppdaterIservDatoHendelse) {
        String sql = "UPDATE UTMELDING SET iserv_fra_dato = ?, oppdatert_dato = CURRENT_TIMESTAMP WHERE aktor_id = ?";
        Timestamp nyIservFraDato = Timestamp.from(oppdaterIservDatoHendelse.getIservFraDato().toInstant());

        db.update(sql, nyIservFraDato, oppdaterIservDatoHendelse.getAktorId().get());

        secureLog.info("ISERV bruker med aktorid {} har blitt oppdatert inn i UTMELDING tabell", oppdaterIservDatoHendelse.getAktorId());
    }

    public void insertUtmeldingTabell(InsertIUtmelding bleIservEvent) {
        Timestamp iservFraTimestamp = Timestamp.from(bleIservEvent.getIservFraDato().toInstant());

        String sql = "INSERT INTO UTMELDING (aktor_id, iserv_fra_dato, oppdatert_dato) VALUES (?, ?, CURRENT_TIMESTAMP)";

        db.update(sql, bleIservEvent.getAktorId().get(), iservFraTimestamp);

        secureLog.info(
                "ISERV bruker med aktorid {} og iserv_fra_dato {} har blitt insertert inn i UTMELDING tabell",
                bleIservEvent.getAktorId(),
                iservFraTimestamp
        );
    }

    public void slettBrukerFraUtmeldingTabell(AktorId aktorId) {
        String sql = "DELETE FROM UTMELDING WHERE aktor_id = ?";

        int rowsDeleted = db.update(sql, aktorId.get());

        if (rowsDeleted > 0) {
            secureLog.info("Aktorid {} har blitt slettet fra UTMELDING tabell", aktorId);
        }
    }

    public List<UtmeldingEntity> finnBrukereMedIservI28Dager() {
        Timestamp tilbake28 = Timestamp.valueOf(LocalDateTime.now().minusDays(28));
        String sql = "SELECT * FROM UTMELDING WHERE aktor_id IS NOT NULL AND iserv_fra_dato < ?";
        return db.query(sql, UtmeldingRepository::mapper, tilbake28);
    }

    private static UtmeldingEntity mapper(ResultSet resultSet, int row) throws SQLException {
        return new UtmeldingEntity(
                resultSet.getString("aktor_id"),
                DbUtils.hentZonedDateTime(resultSet, "iserv_fra_dato")
        );
    }

}
