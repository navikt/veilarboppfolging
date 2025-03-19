package no.nav.veilarboppfolging.repository

import lombok.SneakyThrows
import lombok.extern.slf4j.Slf4j
import no.nav.common.types.identer.AktorId
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.InsertIUtmelding
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.UpdateIservDatoUtmelding
import no.nav.veilarboppfolging.repository.entity.UtmeldingEntity
import no.nav.veilarboppfolging.utils.DbUtils
import no.nav.veilarboppfolging.utils.SecureLog
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.time.LocalDateTime

@Slf4j
@Repository
class UtmeldingRepository @Autowired constructor(
    val db: NamedParameterJdbcTemplate
) {
    fun eksisterendeIservBruker(aktorId: AktorId): UtmeldingEntity? {
        val sql = "SELECT * FROM UTMELDING WHERE aktor_id = :aktorId"
        return db.query(sql, mapOf("aktorId" to aktorId.get()), ::mapper)
            .firstOrNull()
    }

    @SneakyThrows
    fun updateUtmeldingTabell(oppdaterIservDatoHendelse: UpdateIservDatoUtmelding) {
        val sql = "UPDATE UTMELDING SET iserv_fra_dato = :nyIservFraDato, oppdatert_dato = CURRENT_TIMESTAMP WHERE aktor_id = :aktorId"
        val nyIservFraDato = Timestamp.from(oppdaterIservDatoHendelse.iservFraDato.toInstant())

        db.update(sql, mapOf(
            "nyIservFraDato" to nyIservFraDato,
            "aktorId" to oppdaterIservDatoHendelse.aktorId.get()
        ))

        SecureLog.secureLog.info(
            "ISERV bruker med aktorid {} har blitt oppdatert inn i UTMELDING tabell",
            oppdaterIservDatoHendelse.aktorId.get()
        )
    }

    fun insertUtmeldingTabell(bleIservEvent: InsertIUtmelding) {
        val iservFraTimestamp = Timestamp.from(bleIservEvent.iservFraDato.toInstant())
        val sql = "INSERT INTO UTMELDING (aktor_id, iserv_fra_dato, oppdatert_dato) VALUES (:aktorId, :iservFraDato, CURRENT_TIMESTAMP)"
        db.update(sql, mapOf("iservFraDato" to iservFraTimestamp, "aktorId" to bleIservEvent.aktorId.get()))

        SecureLog.secureLog.info(
            "ISERV bruker med aktorid {} og iserv_fra_dato {} har blitt insertert inn i UTMELDING tabell",
            bleIservEvent.aktorId,
            iservFraTimestamp
        )
    }

    fun slettBrukerFraUtmeldingTabell(aktorId: AktorId) {
        val sql = "DELETE FROM UTMELDING WHERE aktor_id = :aktorId"
        val rowsDeleted = db.update(sql, mapOf("aktorId" to aktorId.get()))

        if (rowsDeleted > 0) {
            SecureLog.secureLog.info("Aktorid {} har blitt slettet fra UTMELDING tabell", aktorId)
        }
    }

    fun finnBrukereMedIservI28Dager(): List<UtmeldingEntity> {
        val tilbake28 = Timestamp.valueOf(LocalDateTime.now().minusDays(28))
        val sql = "SELECT * FROM UTMELDING WHERE aktor_id IS NOT NULL AND iserv_fra_dato < :tilbake28"
        return db.query(
            sql,
            mapOf("tilbake28" to tilbake28)
        ) { resultSet, row -> mapper(resultSet, row) }
    }

    fun tellBrukereUnderOppfolgingIGracePeriode(): Int {
        val sql = """
            SELECT count(*) FROM utmelding
            JOIN oppfolgingstatus ON utmelding.aktor_id = oppfolgingstatus.aktor_id
            WHERE oppfolgingstatus.under_oppfolging = 1
        """.trimIndent()
        return db.queryForObject(sql, emptyMap<String, Unit>(), Int::class.java)
    }

    companion object {
        @Throws(SQLException::class)
        private fun mapper(resultSet: ResultSet, row: Int): UtmeldingEntity {
            return UtmeldingEntity(
                resultSet.getString("aktor_id"),
                DbUtils.hentZonedDateTime(resultSet, "iserv_fra_dato")
            )
        }
    }
}