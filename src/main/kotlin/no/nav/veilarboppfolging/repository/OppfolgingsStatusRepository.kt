package no.nav.veilarboppfolging.repository

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Hovedmaal
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.dbutil.toInt
import no.nav.veilarboppfolging.domain.Oppfolging
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity
import no.nav.veilarboppfolging.utils.DbUtils
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.SQLException
import java.util.Optional
import java.util.function.Supplier

@Repository
class OppfolgingsStatusRepository(private val jdbcTemplate: JdbcTemplate) {
    val db = NamedParameterJdbcTemplate(jdbcTemplate)

    fun hentOppfolging(aktorId: AktorId): Optional<OppfolgingEntity> {
        return DbUtils.queryForNullableObject<OppfolgingEntity?>(Supplier {
            db.queryForObject<OppfolgingEntity>(
                "SELECT * FROM OPPFOLGINGSTATUS WHERE aktor_id = :aktorId",
                mapOf("aktorId" to aktorId.get()),
                { rs, row -> map(rs) },
            )
        })
    }

    fun oppdaterArenaOppfolgingStatus(aktorId: AktorId, formidlingsgruppe: Formidlingsgruppe, kvalifiseringsgruppe: Kvalifiseringsgruppe, hovedmaal: Optional<Hovedmaal>, oppfolgingsenhet: Optional<EnhetId>) {
        val params = mapOf(
            "aktorId" to aktorId.get(),
            "formidlingsgruppe" to formidlingsgruppe.name,
            "hovedmaal" to hovedmaal.map { it.name }.orElse(null),
            "kvalifiseringsgruppe" to kvalifiseringsgruppe.name,
            "oppfolgingsenhet" to oppfolgingsenhet.map { it.get() }.orElse(null),
        )
        val sql = """
            UPDATE OPPFOLGINGSTATUS
            SET formidlingsgruppe = :formidlingsgruppe, 
            kvalifiseringsgruppe = :kvalifiseringsgruppe, 
            hovedmaal = :hovedmaal, 
            oppfolgingsenhet = :oppfolgingsenhet 
            WHERE aktor_id = :aktorId
        """.trimIndent()
        db.update(sql, params)
    }

    fun opprettOppfolging(aktorId: AktorId): Oppfolging {
        val params = mapOf(
            "aktorId" to aktorId.get(),
            "underOppfolging" to toInt(false)
        )
        db.update(
            """INSERT INTO OPPFOLGINGSTATUS(aktor_id, under_oppfolging, oppdatert) 
                VALUES(:aktorId, :underOppfolging, CURRENT_TIMESTAMP)""".trimMargin(),
            params,
        )

        // FIXME: return the actual database object.
        return Oppfolging().setAktorId(aktorId.get()).setUnderOppfolging(false)
    }

    fun hentUnikeBrukerePage(offset: Int, pageSize: Int): MutableList<AktorId> {
        val sql = String.format(
            "SELECT DISTINCT aktor_id FROM OPPFOLGINGSTATUS ORDER BY aktor_id OFFSET %d ROWS FETCH NEXT %d ROWS ONLY",
            offset,
            pageSize
        )
        return db.query<String>(sql) { rs, rowNum -> rs.getString("aktor_id") }
            .map { AktorId.of(it) }
            .toMutableList()
    }

    companion object {
        const val GJELDENDE_MAL: String = "gjeldende_mal"
        const val GJELDENDE_MANUELL_STATUS: String = "gjeldende_manuell_status"
        const val AKTOR_ID: String = "aktor_id"
        const val UNDER_OPPFOLGING: String = "under_oppfolging"
        const val TABLE_NAME: String = "OPPFOLGINGSTATUS"
        const val VEILEDER: String = "veileder"
        const val NY_FOR_VEILEDER: String = "ny_for_veileder"
        const val SIST_TILORDNET: String = "sist_tilordnet"
        const val OPPDATERT: String = "oppdatert"

        @Throws(SQLException::class)
        fun map(rs: ResultSet): OppfolgingEntity {
            return OppfolgingEntity()
                .setAktorId(rs.getString(AKTOR_ID))
                .setGjeldendeManuellStatusId(rs.getLong(GJELDENDE_MANUELL_STATUS))
                .setGjeldendeMaalId(rs.getLong(GJELDENDE_MAL))
                .setGjeldendeKvpId(rs.getLong("gjeldende_kvp"))
                .setVeilederId(rs.getString(VEILEDER))
                .setFormidlingsgruppe(rs.getStringOrNull("formidlingsgruppe")?.let(Formidlingsgruppe::valueOf))
                .setKvalifiseringsgruppe(rs.getStringOrNull("kvalifiseringsgruppe")?.let(Kvalifiseringsgruppe::valueOf))
                .setHovedmaal(rs.getStringOrNull("hovedmaal")?.let(Hovedmaal::valueOf))
                .setUnderOppfolging(rs.getBoolean(UNDER_OPPFOLGING))
                .setEnhetId(rs.getString("oppfolgingsenhet")?.let { EnhetId(it) })
        }
    }
}

fun ResultSet.getStringOrNull(key: String): String? {
    return getString(key)
}