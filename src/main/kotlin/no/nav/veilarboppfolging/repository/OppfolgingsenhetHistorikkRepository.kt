package no.nav.veilarboppfolging.repository

import no.nav.common.types.identer.AktorId
import no.nav.veilarboppfolging.repository.entity.OppfolgingsenhetEndringEntity
import no.nav.veilarboppfolging.utils.DbUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.SQLException

@Repository
class OppfolgingsenhetHistorikkRepository (val db: NamedParameterJdbcTemplate) {

    fun insertOppfolgingsenhetEndringForAktorId(aktorId: AktorId, enhet: String) {
        val params = mapOf(
            "aktorId" to aktorId.get(),
            "enhet" to enhet,
            "ENHET_SEQ" to DbUtils.nesteFraSekvens(db, "ENHET_SEQ")
        )
        val sql =
            """INSERT INTO OPPFOLGINGSENHET_ENDRET (aktor_id, enhet, endret_dato, enhet_seq) 
                VALUES(:aktorId, :enhet, CURRENT_TIMESTAMP, :ENHET_SEQ)""".trimMargin()
        db.update(sql, params)
    }

    fun hentOppfolgingsenhetEndringerForAktorId(aktorId: AktorId): MutableList<OppfolgingsenhetEndringEntity?> {
        val params = mapOf("aktorId" to aktorId.get())
        val sql = "SELECT enhet, endret_dato FROM OPPFOLGINGSENHET_ENDRET WHERE aktor_id = :aktorId ORDER BY enhet_seq DESC"
        return db.query(sql, params) { resultset, rows ->
            resultset.toOppfolgingsenhetEndringEntity()
        }
    }

    fun hentArenaOppfolgingsenhetForAktorId(aktorId: AktorId): OppfolgingsenhetEndringEntity? {
        val params = mapOf("aktorId" to aktorId.get())
        val sql =
            """SELECT distinct on (aktor_id) enhet, endret_dato FROM OPPFOLGINGSENHET_ENDRET 
                WHERE aktor_id = :aktorId ORDER BY aktor_id, enhet_seq DESC""".trimMargin()
        return db.query(sql, params) { resultset, rows ->
            resultset.toOppfolgingsenhetEndringEntity()
        }.firstOrNull()
    }

    fun ResultSet.toOppfolgingsenhetEndringEntity(): OppfolgingsenhetEndringEntity {
        return OppfolgingsenhetEndringEntity.builder()
            .enhet(this.getString("enhet"))
            .endretDato(DbUtils.hentZonedDateTime(this, "endret_dato"))
            .build()
    }
}