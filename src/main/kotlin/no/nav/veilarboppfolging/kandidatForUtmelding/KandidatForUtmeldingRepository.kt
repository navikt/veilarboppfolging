package no.nav.veilarboppfolging.kandidatForUtmelding

import no.nav.common.types.identer.AktorId
import no.nav.veilarboppfolging.repository.SakEntity
import no.nav.veilarboppfolging.repository.SakEntity.Companion.fromResultSet
import no.nav.veilarboppfolging.utils.DbUtils.hentZonedDateTime
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.ZonedDateTime
import java.util.UUID

@Repository
class KandidatForUtmeldingRepository(
    private val db: NamedParameterJdbcTemplate
) {

    fun lagreKandidat(hendelse: KandidatForUtmeldingHendelse) {
        val sql = """
            INSERT INTO kandidat_for_utmelding(aktor_id, hendelse, avsluttet_av, kilde, aarsak)
            VALUES (:aktorId, :hendelse, :avsluttet_av, :kilde, :aarsak)
            ON CONFLICT (aktor_id) DO NOTHING
        """.trimIndent()
        db.update(sql, mapOf(
            "aktorId" to hendelse.aktorId.get(),
            "hendelse" to hendelse.type.name,
            "avsluttet_av" to hendelse.avsluttetAv.name,
            "kilde" to hendelse.kilde,
            "aarsak" to hendelse.aarsak
        ))
    }

    fun fjernKandidat(aktorId: AktorId) {
        val sql = """
            DELETE FROM kandidat_for_utmelding
            WHERE aktor_id = :aktorId
        """.trimIndent()
        db.update(sql, mapOf("aktorId" to aktorId.get()))
    }

    fun hentKandidat(aktorId: AktorId): KandidatForUtmeldingHendelse? {



        return db.query("""
            SELECT * FROM SAK WHERE OPPFOLGINGSPERIODE_UUID = :oppfølgingsperiodeUUID
        """.trimIndent(),
            mapOf("oppfølgingsperiodeUUID" to oppfølgingsperiodeUUID.toString()),
            SakEntity::fromResultSet
        )
    }

    data class SakEntity(
        val id: Long,
        val oppfølgingsperiodeUUID: UUID,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun fromResultSet(resultSet: ResultSet, row: Int): SakEntity = SakEntity(
                id = resultSet.getLong("id"),
                oppfølgingsperiodeUUID = UUID.fromString(resultSet.getString("oppfolgingsperiode_uuid")),
                createdAt = hentZonedDateTime(resultSet, "created_at"),
            )
        }
    }
}