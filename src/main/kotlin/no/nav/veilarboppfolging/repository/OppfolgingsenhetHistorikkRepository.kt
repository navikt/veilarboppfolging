package no.nav.veilarboppfolging.repository

import java.sql.ResultSet
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.veilarboppfolging.repository.entity.OppfolgingsenhetEndringEntity
import no.nav.veilarboppfolging.utils.DbUtils
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class OppfolgingsenhetHistorikkRepository (val db: NamedParameterJdbcTemplate) {

    fun insertOppfolgingsenhetEndringForAktorId(aktorId: AktorId, enhet: EnhetId) {
        val params = mapOf(
            "aktorId" to aktorId.get(),
            "enhet" to enhet.get(),
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

    fun ResultSet.toOppfolgingsenhetEndringEntity(): OppfolgingsenhetEndringEntity {
        return OppfolgingsenhetEndringEntity.builder()
            .enhet(this.getString("enhet"))
            .endretDato(DbUtils.hentZonedDateTime(this, "endret_dato"))
            .build()
    }
}
