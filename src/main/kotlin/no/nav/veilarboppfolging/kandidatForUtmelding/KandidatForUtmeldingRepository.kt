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

    fun lagreKandidat(hendelse: KandidatForUtmeldingHendelse): UUID {
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
        return hendelseId
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

    fun hentAktivKandidat(oppfolgingsperiodeId: UUID): AktivKandidatForUtmelding? {
        return db.query(
            """
            SELECT oppfolgingsperiode_uuid, siste_utmeldingshendelse_id
            FROM kandidater_for_utmelding
            WHERE oppfolgingsperiode_uuid = :oppfolgingsperiodeId
              AND forlenget_til IS NULL
            """.trimIndent(),
            mapOf("oppfolgingsperiodeId" to oppfolgingsperiodeId.toString()),
        ) { rs, _ ->
            AktivKandidatForUtmelding(
                oppfolgingsperiodeUuid = UUID.fromString(rs.getString("oppfolgingsperiode_uuid")),
                sisteUtmeldingshendelseId = UUID.fromString(rs.getString("siste_utmeldingshendelse_id"))
            )
        }.firstOrNull()
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

    fun hentSisteKandidatForUtmeldingHendelseMedId(oppfolgingsperiodeId: UUID): KandidatForUtmeldingHendelseMedId? {
        return db.query(
            """
            SELECT *
            FROM kandidater_for_utmelding_hendelser
            WHERE oppfolgingsperiode_uuid = :oppfolgingsperiodeId order by created_at desc
            LIMIT 1
            """.trimIndent(),
            mapOf("oppfolgingsperiodeId" to oppfolgingsperiodeId.toString()),
        ) { rs, _ ->
            KandidatForUtmeldingHendelseMedId(
                utmeldingshendelseId = UUID.fromString(rs.getString("utmeldingshendelse_id")),
                hendelse = map(rs)
            )
        }.firstOrNull()
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

    fun lagreKafkaPublisering(logg: KandidatForUtmeldingKafkaPublisering) {
        val sql = """
            INSERT INTO kandidater_for_utmelding_kafka_logg(
              id,
              utmeldingshendelse_id,
              publiseringstype,
              status,
              kafka_topic,
              kafka_partition,
              kafka_offset,
              feilmelding
            ) VALUES (
              gen_random_uuid(),
              :utmeldingshendelseId,
              :publiseringstype,
              :status,
              :kafkaTopic,
              :kafkaPartition,
              :kafkaOffset,
              :feilmelding
            )
        """.trimIndent()

        db.update(
            sql,
            mapOf(
                "utmeldingshendelseId" to logg.utmeldingshendelseId,
                "publiseringstype" to logg.publiseringstype.name,
                "status" to logg.status.name,
                "kafkaTopic" to logg.kafkaTopic,
                "kafkaPartition" to logg.kafkaPartition,
                "kafkaOffset" to logg.kafkaOffset,
                "feilmelding" to logg.feilmelding,
            )
        )
    }

    fun hentKafkaPubliseringer(utmeldingshendelseId: UUID): List<KandidatForUtmeldingKafkaPublisering> {
        return db.query(
            """
            SELECT utmeldingshendelse_id, publiseringstype, status, kafka_topic, kafka_partition, kafka_offset, feilmelding
            FROM kandidater_for_utmelding_kafka_logg
            WHERE utmeldingshendelse_id = :utmeldingshendelseId
            ORDER BY opprettet_tidspunkt ASC
            """.trimIndent(),
            mapOf("utmeldingshendelseId" to utmeldingshendelseId),
        ) { rs, _ ->
            KandidatForUtmeldingKafkaPublisering(
                utmeldingshendelseId = UUID.fromString(rs.getString("utmeldingshendelse_id")),
                publiseringstype = KandidatForUtmeldingKafkaPubliseringstype.valueOf(rs.getString("publiseringstype")),
                status = KandidatForUtmeldingKafkaPubliseringStatus.valueOf(rs.getString("status")),
                kafkaTopic = rs.getString("kafka_topic"),
                kafkaPartition = rs.getInt("kafka_partition").let { partition ->
                    if (rs.wasNull()) null else partition
                },
                kafkaOffset = rs.getLong("kafka_offset").let { offset ->
                    if (rs.wasNull()) null else offset
                },
                feilmelding = rs.getString("feilmelding"),
            )
        }
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