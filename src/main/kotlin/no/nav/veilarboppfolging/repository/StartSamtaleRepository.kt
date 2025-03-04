package no.nav.veilarboppfolging.repository

import lombok.RequiredArgsConstructor
import no.nav.common.types.identer.AktorId
import no.nav.veilarboppfolging.domain.StartSamtale
import no.nav.veilarboppfolging.utils.DatabaseUtils
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@RequiredArgsConstructor
@Repository
open class StartSamtaleRepository(
    private val jdbc: NamedParameterJdbcTemplate
) {

    fun oppdaterSisteStartSamtaleInnsending(aktorId: AktorId) {
        val sql = """
            INSERT INTO SISTE_START_SAMTALE_INNSENDING(DATO, AKTOR_ID) VALUES(CURRENT_TIMESTAMP, :aktorId)
        """.trimIndent()
        val params = MapSqlParameterSource().apply {
            addValue("aktorId", aktorId.get())
        }
        val keyHolder = GeneratedKeyHolder()

        try {
            jdbc.update(sql, params, keyHolder, arrayOf("id"))
        } catch (e: DuplicateKeyException) {
            val updateSql = """
                UPDATE SISTE_START_SAMTALE_INNSENDING SET DATO = CURRENT_TIMESTAMP WHERE AKTOR_ID = :aktorId
            """.trimIndent()
            jdbc.update(updateSql, params)
        }
    }

    fun hentSisteStartSamtaleInnsending(aktorId: AktorId): StartSamtale? {
        val sql = """
            SELECT * FROM SISTE_START_SAMTALE_INNSENDING WHERE AKTOR_ID = :aktorId
        """.trimIndent()
        val params = mapOf("aktorId" to aktorId.get())
        return try {
            jdbc.queryForObject(sql, params, rowMapper)
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }

    open val rowMapper = RowMapper { rs: ResultSet, rowNum: Int ->
        StartSamtale(dato = DatabaseUtils.hentZonedDateTime(rs, "DATO"))
    }
}