package no.nav.veilarboppfolging.kandidatForUtmelding

import java.sql.ResultSet
import java.util.UUID
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.AvsluttetAarsakType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class KandidatForUtmeldingRepository(
    private val db: NamedParameterJdbcTemplate
) {

    fun lagreKandidat(hendelse: KandidatForUtmeldingHendelse) {
        val sql = """
            INSERT INTO kandidat_for_utmelding(aktor_id, hendelse, avsluttet_av, kilde, detaljer, fnr, oppfolgingsperiode_uuid)
            VALUES (:aktorId, :hendelse, :avsluttet_av, :kilde, :detaljer, :fnr, :oppfolgingsperiode_uuid)
            ON CONFLICT (aktor_id) DO NOTHING
        """.trimIndent()
        db.update(
            sql, mapOf(
                "aktorId" to hendelse.aktorId.get(),
                "hendelse" to hendelse.type.name,
                "avsluttet_av" to hendelse.avsluttetAv.name,
                "kilde" to hendelse.kilde,
                "detaljer" to hendelse.detaljer,
                "fnr" to hendelse.fnr.get(),
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