package no.nav.veilarboppfolging.kandidatForUtmelding

import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import no.nav.common.json.JsonUtils
import no.nav.veilarboppfolging.repository.getStringOrNull
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class KandidatForUtmeldingRepository(
    private val db: NamedParameterJdbcTemplate
) {

    fun lagreKandidat(hendelse: KandidatForUtmeldingHendelse) {
        val hendelseId = insertUtmeldingsHendelse(hendelse)
        val sql = """
            INSERT INTO kandidater_for_utmelding(siste_utmeldingshendelse_id, oppfolgingsperiode_uuid, forlenget_til)
            VALUES (:hendelseId, :oppfolgingsperiodeId, null)
            ON CONFLICT (oppfolgingsperiode_uuid) 
            DO UPDATE SET updated_at = current_timestamp, siste_utmeldingshendelse_id = :hendelseId
        """.trimIndent()
        db.update(
            sql, mapOf(
                "oppfolgingsperiodeId" to hendelse.oppfolgingsperiodeUuid,
                "hendelseId" to hendelseId,
                "avsluttetAv" to hendelse.utfortAvType.name,
            )
        )
    }

    private fun insertUtmeldingsHendelse(hendelse: KandidatForUtmeldingHendelse): UUID {
        val sql = """
            INSERT INTO kandidater_for_utmelding_hendelser(utmeldingshendelse_id, hendelse, hendelse_data, utfort_av, utfort_av_type, kilde, oppfolgingsperiode_uuid, hendelse_tidspunkt)
            VALUES (gen_random_uuid(), :hendelse, :hendelseData::jsonb, :utfortAv, :utfortAvType, :kilde, :oppfolgingsperiode_uuid, :hendelseTidspunkt)
            RETURNING utmeldingshendelse_id
        """.trimIndent()
        return db.queryForObject(
            sql, mapOf(
                "hendelse" to hendelse.type.name,
                "hendelseData" to hendelse.hendelseDataJson,
                "utfortAv" to hendelse.utfortAv,
                "utfortAvType" to hendelse.utfortAvType.name,
                "kilde" to hendelse.kilde,
                "oppfolgingsperiode_uuid" to hendelse.oppfolgingsperiodeUuid,
                "hendelseTidspunkt" to Timestamp.valueOf(
                    LocalDateTime.ofInstant(hendelse.hendelseTidspunkt, ZoneOffset.UTC)
                )
            )
        ) { rs, _ -> UUID.fromString(rs.getString("utmeldingshendelse_id")) }!!
    }

    fun fjernKandidat(oppfolgingsperiodeId: UUID) {
        val sql = """
            DELETE FROM kandidater_for_utmelding
            WHERE oppfolgingsperiode_uuid = :oppfolgingsperiodeId
        """.trimIndent()
        db.update(sql, mapOf("oppfolgingsperiodeId" to oppfolgingsperiodeId.toString()))
    }

    fun hentKandidat(oppfolgingsperiodeId: UUID): KandidatForUtmeldingHendelse? {
        return db.query(
            """
            SELECT kfuh.*
            FROM kandidater_for_utmelding kfu
            JOIN kandidater_for_utmelding_hendelser kfuh ON kfu.siste_utmeldingshendelse_id = kfuh.utmeldingshendelse_id
            WHERE kfu.oppfolgingsperiode_uuid = :oppfolgingsperiodeId AND kfu.forlenget_til IS NULL
            """.trimIndent(),
            mapOf("oppfolgingsperiodeId" to oppfolgingsperiodeId.toString()),
        ) { rs, _ -> map(rs) }
            .firstOrNull()
    }

    // for tester
    fun hentKandidatMedForlengelse(oppfolgingsperiodeId: UUID): KandidatForUtmeldingHendelse? {
        return db.query(
            """
            SELECT kfuh.*
            FROM kandidater_for_utmelding kfu
            JOIN kandidater_for_utmelding_hendelser kfuh ON kfu.siste_utmeldingshendelse_id = kfuh.utmeldingshendelse_id
            WHERE kfu.oppfolgingsperiode_uuid = :oppfolgingsperiodeId AND kfu.forlenget_til IS NOT NULL
            """.trimIndent(),
            mapOf("oppfolgingsperiodeId" to oppfolgingsperiodeId.toString()),
        ) { rs, _ -> map(rs) }
            .firstOrNull()
    }

    fun hentSisteKandidatForUtmeldingHendelse(oppfolgingsperiodeId: UUID): KandidatForUtmeldingHendelse? {
        return db.query(
            """
            SELECT *
            FROM kandidater_for_utmelding_hendelser
            WHERE oppfolgingsperiode_uuid = :oppfolgingsperiodeId order by created_at desc
            LIMIT 1
            """.trimIndent(),
            mapOf("oppfolgingsperiodeId" to oppfolgingsperiodeId.toString()),
        ) { rs, _ -> map(rs) }
            .firstOrNull()
    }

    fun hentAktiveKandidater(offset: Int, batchSize: Int): List<KandidatForUtmeldingHendelse> {
        return db.query(
            """
            SELECT kfuh.*
            FROM kandidater_for_utmelding kfu
            JOIN kandidater_for_utmelding_hendelser kfuh ON kfu.siste_utmeldingshendelse_id = kfuh.utmeldingshendelse_id
            WHERE kfu.forlenget_til IS NULL
            ORDER BY kfu.created_at
            OFFSET :offset ROWS FETCH NEXT :batchSize ROWS ONLY
            """.trimIndent(),
            mapOf(
                "offset" to offset,
                "batchSize" to batchSize
            ),
        ) { rs, _ -> map(rs) }
    }

    fun hentKandidaterMedUtloptForlengelse(): List<KandidatForUtmeldingHendelse> {
        return db.query(
            """
            SELECT kfuh.*
            FROM kandidater_for_utmelding kfu
            JOIN kandidater_for_utmelding_hendelser kfuh ON kfu.siste_utmeldingshendelse_id = kfuh.utmeldingshendelse_id
            WHERE kfu.forlenget_til IS NOT NULL AND kfu.forlenget_til < current_timestamp
            """.trimIndent(),
        ) { rs, _ -> map(rs) }
    }

    fun nullstillForlengetTil(oppfolgingsperiodeId: UUID) {
        val sql = """
            UPDATE kandidater_for_utmelding
            SET forlenget_til = NULL
            WHERE oppfolgingsperiode_uuid = :oppfolgingsperiodeId
        """.trimIndent()
        db.update(sql, mapOf("oppfolgingsperiodeId" to oppfolgingsperiodeId.toString()))
    }

    fun hentAntallKandidaterForUtmelding(): Int {
        val sql = """
            SELECT COUNT(*) AS antall
            FROM kandidater_for_utmelding
        """.trimIndent()
        return db.queryForObject(sql, emptyMap<String, Any>()) { rs, _ -> rs.getInt("antall") } ?: 0
    }

    fun hentEllerOpprettFilterhendelseId(oppfolgingsperiodeId: UUID): UUID {
        val sql = """
            INSERT INTO filterkategori_id_mapping(kategori, oppfolgingsperiode_id, filterkategori_person_id)
            VALUES ('KANDIDAT_FOR_UTMELDING', :oppfolgingsperiodeId, gen_random_uuid())
            ON CONFLICT (oppfolgingsperiode_id, kategori) 
            DO UPDATE SET oppfolgingsperiode_id = EXCLUDED.oppfolgingsperiode_id
            RETURNING filterkategori_person_id
        """.trimIndent()
        return db.queryForObject(
            sql, mapOf(
                "oppfolgingsperiodeId" to oppfolgingsperiodeId.toString(),
            )
        ) { rs, _ -> UUID.fromString(rs.getString("filterkategori_person_id")) }!!
    }

    fun hentFilterhendelseId(oppfolgingsperiodeId: UUID): UUID? {
        return db.query(
            """
            SELECT filterkategori_person_id
            FROM filterkategori_id_mapping
            WHERE kategori = 'KANDIDAT_FOR_UTMELDING' AND oppfolgingsperiode_id = :oppfolgingsperiodeId
        """.trimIndent(),
            mapOf(
                "oppfolgingsperiodeId" to oppfolgingsperiodeId.toString(),
            ),
        ) { rs, _ -> UUID.fromString(rs.getString("filterkategori_person_id")) }.firstOrNull()
    }

    fun map(resultSet: ResultSet): KandidatForUtmeldingHendelse {
        val type = KandidatForUtmeldingHendelseType.valueOf(resultSet.getString("hendelse"))
        return when (type) {
            KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET,
            KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
            KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE -> resultSet.toArbeidssøkerPeriodeAvsluttet()
        }
    }
}

fun ResultSet.toArbeidssøkerPeriodeAvsluttet() = ArbeidssøkerPeriodeAvsluttet(
    oppfolgingsperiodeUuid = UUID.fromString(getString("oppfolgingsperiode_uuid")),
    utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.valueOf(getString("utfort_av_type")),
    utfortAv = getString("utfort_av"),
    kilde = getString("kilde"),
    hendelseTidspunkt = getTimestamp("hendelse_tidspunkt").toLocalDateTime().toInstant(ZoneOffset.UTC),
    avslutningsarsak = getStringOrNull("hendelse_data")?.let { JsonUtils.fromJson(it, ArbeidssøkerPeriodeAvsluttet.Detaljer::class.java).avslutningsarsak },
    kandidatForUtmeldingHendelseType = KandidatForUtmeldingHendelseType.valueOf(getString("hendelse"))
)