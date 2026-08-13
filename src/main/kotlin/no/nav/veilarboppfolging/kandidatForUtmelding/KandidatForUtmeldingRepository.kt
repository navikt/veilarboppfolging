package no.nav.veilarboppfolging.kandidatForUtmelding

import java.sql.ResultSet
import java.util.UUID
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class KandidatForUtmeldingRepository(
    private val db: NamedParameterJdbcTemplate
) {

    fun lagreKandidat(hendelse: KandidatForUtmeldingHendelse) {
        val hendelseId = insertUtmeldingsHendelse(hendelse)
        val sql = """
            INSERT INTO kandidater_for_utmelding(aktor_id, siste_utmeldingshendelse_id, oppfolgingsperiode_uuid, forlenget_til)
            VALUES (:aktorId, :hendelseId, :oppfolgingsperiodeId, null)
            ON CONFLICT (oppfolgingsperiode_uuid) 
            DO UPDATE SET updated_at = current_timestamp, siste_utmeldingshendelse_id = :hendelseId
        """.trimIndent()
        db.update(
            sql, mapOf(
                "oppfolgingsperiodeId" to hendelse.oppfolgingsperiodeUuid,
                "hendelseId" to hendelseId,
                "avsluttetAv" to hendelse.avsluttetAv.name,
            )
        )
    }

    private fun insertUtmeldingsHendelse(hendelse: KandidatForUtmeldingHendelse): UUID {
        val sql = """
            INSERT INTO kandidater_for_utmelding(gen_random_uuid(), utfoert_av, utfoert_av_type, kilde, hendelse_detaljer, oppfolgingsperiode_uuid)
            VALUES (:hendelse, :utfoertAv, :utfoertAvType, :kilde, :detaljer, :oppfolgingsperiode_uuid)
        """.trimIndent()
        db.update(
            sql, mapOf(
                "hendelse" to hendelse.type.name,
                "utfoertAv" to hendelse.avsluttetAv.utfoertAv.name,
                "utfoertAvType" to hendelse.avsluttetAv.utfoertAvType.name, 
                "kilde" to hendelse.kilde,
                "detaljer" to hendelse.detaljer,
                "oppfolgingsperiode_uuid" to hendelse.oppfolgingsperiodeUuid
            )
        )
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
            SELECT * FROM kandidat_for_utmelding 
            WHERE aktor_id = :aktor_id
            """.trimIndent(),
            mapOf("aktor_id" to aktorId.get()),
        ) { rs, _ -> map(rs) }
            .firstOrNull()
    }

    fun map(resultSet: ResultSet): KandidatForUtmeldingHendelse {
        val type = KandidatForUtmeldingHendelseType.valueOf(resultSet.getString("hendelse"))
        return when (type) {
            KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET,
            KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
            KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE -> ArbeidssøkerPeriodeAvsluttet(
                aktorId = AktorId.of(resultSet.getString("aktor_id")),
                fnr = Fnr.of(resultSet.getString("fnr")),
                oppfolgingsperiodeUuid = UUID.fromString(resultSet.getString("oppfolgingsperiode_uuid")),
                avsluttetAv = KandidatForUtmeldingHendelseAvsluttetAv.valueOf(resultSet.getString("avsluttet_av")),
                kilde = resultSet.getString("kilde"),
                detaljer = resultSet.getString("detaljer"),
                kandidatForUtmeldingHendelseType = type
            )
        }
    }
}