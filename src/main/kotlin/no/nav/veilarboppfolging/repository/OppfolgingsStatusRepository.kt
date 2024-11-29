package no.nav.veilarboppfolging.repository

import no.nav.common.types.identer.AktorId
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Hovedmaal
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.dbutil.toInt
import no.nav.veilarboppfolging.domain.Oppfolging
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity
import no.nav.veilarboppfolging.utils.DbUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.SQLException
import java.util.Optional
import java.util.function.Supplier
import java.util.stream.Collectors

@Repository
class OppfolgingsStatusRepository(private val db: JdbcTemplate) {
    fun hentOppfolging(aktorId: AktorId): Optional<OppfolgingEntity> {
        return DbUtils.queryForNullableObject<OppfolgingEntity?>(Supplier {
            db.queryForObject<OppfolgingEntity>(
                "SELECT * FROM OPPFOLGINGSTATUS WHERE aktor_id = ?",
                { rs, row -> map(rs) },
                aktorId.get()
            )
        })
    }

    fun opprettOppfolging(aktorId: AktorId): Oppfolging {
        db.update(
            "INSERT INTO OPPFOLGINGSTATUS(" +
                    "aktor_id, " +
                    "under_oppfolging, " +
                    "oppdatert) " +
                    "VALUES(?, ?, CURRENT_TIMESTAMP)",
            aktorId.get(),
            toInt(false)
        ) // TODO: Hvorfor false her?

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
        }
    }
}

fun ResultSet.getStringOrNull(key: String): String? {
    return getString(key)
}