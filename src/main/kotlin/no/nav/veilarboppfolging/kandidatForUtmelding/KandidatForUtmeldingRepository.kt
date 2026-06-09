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
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.AvregistreringsType
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository.Companion.map

@Repository
class KandidatForUtmeldingRepository(
    private val db: NamedParameterJdbcTemplate
) {

    fun lagreKandidat(hendelse: KandidatForUtmeldingHendelse) {
        val sql = """
            INSERT INTO kandidat_for_utmelding(aktor_id, hendelse, avsluttet_av, kilde, aarsak, fnr)
            VALUES (:aktorId, :hendelse, :avsluttet_av, :kilde, :aarsak, :fnr)
            ON CONFLICT (aktor_id) DO NOTHING
        """.trimIndent()
        db.update(sql, mapOf(
            "aktorId" to hendelse.aktorId.get(),
            "hendelse" to hendelse.type.name,
            "avsluttet_av" to hendelse.avsluttetAv.name,
            "kilde" to hendelse.kilde,
            "aarsak" to hendelse.aarsak,
            "fnr" to hendelse.fnr.get()
        ))
    }

    fun markerAtOppfolgingBleAvsluttet(aktorId: AktorId, avregistreringsType: AvregistreringsType) {
        val sql = """
            UPDATE kandidat_for_utmelding
            SET avregistrering_type = :avregistrering_type,
            oppfolging_avsluttet_tidspunkt = :oppfolging_avsluttet_tidspunkt
            WHERE aktor_id = :aktor_id
        """.trimIndent()
        db.update(sql, mapOf(
            "avregistrering_type" to avregistreringsType.name,
            "oppfolging_avsluttet_tidspunkt" to ZonedDateTime.now().toOffsetDateTime(),
            "aktor_id" to aktorId.get(),
            ))
    }

    fun fjernKandidat(aktorId: AktorId) {
        val sql = """
            DELETE FROM kandidat_for_utmelding
            WHERE aktor_id = :aktorId
        """.trimIndent()
        db.update(sql, mapOf("aktorId" to aktorId.get()))
    }

    fun hentKandidat(aktorId: AktorId): KandidatForUtmelding? {
        return db.query(
            """
            SELECT * FROM kandidat_for_utmelding WHERE aktor_id = :aktor_id
            """.trimIndent(),
            mapOf("aktor_id" to aktorId.get()),
        ) { rs, _ -> map(rs) }
            .firstOrNull()
    }

    fun map(resultSet: ResultSet): KandidatForUtmelding {
        val type = KandidatForUtmeldingHendelseType.valueOf(resultSet.getString("hendelse"))
        return when (type) {
            KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET -> KandidatForUtmelding(
                aktorId = AktorId.of(resultSet.getString("aktor_id")),
                fnr = Fnr.of(resultSet.getString("fnr")),
                avsluttetAv = KandidatForUtmeldingHendelseAvsluttetAv.valueOf(resultSet.getString("avsluttet_av")),
                kilde = resultSet.getString("kilde"),
                aarsak = resultSet.getString("aarsak"),
                avregistreringsType = resultSet.getString("avregistrering_type")?.let { AvregistreringsType.valueOf(it) },
                oppfolgingAvsluttetTidspunkt = hentZonedDateTime(resultSet, "oppfolging_avsluttet_tidspunkt"),
            )
        }
    }
}