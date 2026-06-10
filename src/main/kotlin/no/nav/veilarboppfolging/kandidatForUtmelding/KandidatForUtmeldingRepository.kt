package no.nav.veilarboppfolging.kandidatForUtmelding

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.AvregistreringsType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.ZonedDateTime
import java.util.*

@Repository
class KandidatForUtmeldingRepository(
    private val db: NamedParameterJdbcTemplate
) {

    fun lagreKandidat(hendelse: KandidatForUtmeldingHendelse) {
        val sql = """
            INSERT INTO kandidat_for_utmelding(aktor_id, hendelse, avsluttet_av, kilde, aarsak, fnr, oppfolgingsperiode_uuid)
            VALUES (:aktorId, :hendelse, :avsluttet_av, :kilde, :aarsak, :fnr, :oppfolgingsperiode_uuid)
            ON CONFLICT (aktor_id) DO NOTHING
        """.trimIndent()
        db.update(sql, mapOf(
            "aktorId" to hendelse.aktorId.get(),
            "hendelse" to hendelse.type.name,
            "avsluttet_av" to hendelse.avsluttetAv.name,
            "kilde" to hendelse.kilde,
            "aarsak" to hendelse.aarsak,
            "fnr" to hendelse.fnr.get(),
            "oppfolgingsperiode_uuid" to hendelse.oppfolgingsperiodeUuid
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

    fun hentKandidat(aktorId: AktorId): KandidatForUtmeldingHendelse? {
        return db.query(
            """
            SELECT * FROM kandidat_for_utmelding WHERE aktor_id = :aktor_id
            """.trimIndent(),
            mapOf("aktor_id" to aktorId.get()),
        ) { rs, _ -> map(rs) }
            .firstOrNull()
    }

    fun map(resultSet: ResultSet): KandidatForUtmeldingHendelse {
        val type = KandidatForUtmeldingHendelseType.valueOf(resultSet.getString("hendelse"))
        return when (type) {
            KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET -> ArbeidssøkerPeriodeAvsluttet(
                aktorId = AktorId.of(resultSet.getString("aktor_id")),
                fnr = Fnr.of(resultSet.getString("fnr")),
                oppfolgingsperiodeUuid = resultSet.getString("oppfolgingsperiode_uuid")?.let { UUID.fromString(it) },
                avsluttetAv = KandidatForUtmeldingHendelseAvsluttetAv.valueOf(resultSet.getString("avsluttet_av")),
                kilde = resultSet.getString("kilde"),
                aarsak = resultSet.getString("aarsak")
            )
        }
    }
}