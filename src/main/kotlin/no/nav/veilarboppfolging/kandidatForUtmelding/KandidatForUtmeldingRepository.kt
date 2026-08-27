package no.nav.veilarboppfolging.kandidatForUtmelding

import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import no.nav.common.json.JsonUtils
import no.nav.veilarboppfolging.repository.getStringOrNull
import org.jetbrains.annotations.TestOnly
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import kotlin.collections.firstOrNull

@Repository
class KandidatForUtmeldingRepository(
    private val db: NamedParameterJdbcTemplate
) {

    fun lagreKandidat(hendelse: KandidatForUtmeldingHendelse) {
        val hendelseId = insertUtmeldingsHendelse(hendelse)
        val avsluttesAutomatiskDato = hendelse.beregnAvsluttesAutomatiskDato()

        val forlengetTil = when (hendelse) {
            is ForlengelseHendelse -> hendelse.hentForlengetTil()
            else -> null
        }
        val sql = """
            INSERT INTO kandidater_for_utmelding(siste_utmeldingshendelse_id, oppfolgingsperiode_uuid, forlenget_til, avsluttes_automatisk_dato)
            VALUES (:hendelseId, :oppfolgingsperiodeId, :forlengetTil, :avsluttesAutomatiskDato)
            ON CONFLICT (oppfolgingsperiode_uuid) 
            DO UPDATE SET updated_at = current_timestamp, siste_utmeldingshendelse_id = :hendelseId, forlenget_til = :forlengetTil, avsluttes_automatisk_dato = :avsluttesAutomatiskDato
        """.trimIndent()
        db.update(
            sql, mapOf(
                "oppfolgingsperiodeId" to hendelse.oppfolgingsperiodeUuid,
                "hendelseId" to hendelseId,
                "forlengetTil" to forlengetTil?.let { Timestamp.valueOf(it.atTime(4, 0)) },
                "avsluttesAutomatiskDato" to avsluttesAutomatiskDato?.let { Timestamp.valueOf(it) },
            )
        )
    }

    private fun insertUtmeldingsHendelse(hendelse: KandidatForUtmeldingHendelse): UUID {
        val sql = """
            INSERT INTO kandidater_for_utmelding_hendelser(utmeldingshendelse_id, hendelse, hendelse_data, utfort_av, utfort_av_type, kilde, oppfolgingsperiode_uuid, hendelse_tidspunkt)
            VALUES (gen_random_uuid(), :hendelse, :hendelseData::jsonb, :utfortAv, :utfortAvType, :kilde, :oppfolgingsperiode_uuid, :hendelseTidspunkt)
            RETURNING utmeldingshendelse_id
        """.trimIndent()
        val type = hendelse.type
        return db.queryForObject(
            sql, mapOf(
                "hendelse" to when (type) {
                    is ArbeidssokerperiodeAvsluttetHendelseType -> type.name
                    is ForlengelseHendelseType -> type.name
                },
                "hendelseData" to hendelse.hendelseDataJson,
                "utfortAv" to hendelse.utfortAv,
                "utfortAvType" to hendelse.utfortAvType.name,
                "kilde" to hendelse.kilde,
                "oppfolgingsperiode_uuid" to hendelse.oppfolgingsperiodeUuid,
                "hendelseTidspunkt" to Timestamp.valueOf(
                    LocalDateTime.ofInstant(hendelse.hendelseTidspunkt, ZoneOffset.UTC)
                ),
            )
        ) { rs, _ -> UUID.fromString(rs.getString("utmeldingshendelse_id")) }!!
    }

    /**
     * Setter avsluttes_automatisk_dato på en kandidat dersom den mangler (f.eks. for kandidater opprettet
     * før feltet ble innført). Idempotent - overskriver ikke en allerede satt dato.
     */
    fun settAvsluttesAutomatiskDatoHvisMangler(oppfolgingsperiodeId: UUID, avsluttesAutomatiskDato: LocalDateTime) {
        val sql = """
            UPDATE kandidater_for_utmelding
            SET avsluttes_automatisk_dato = :avsluttesAutomatiskDato, updated_at = current_timestamp
            WHERE oppfolgingsperiode_uuid = :oppfolgingsperiodeId AND avsluttes_automatisk_dato IS NULL
        """.trimIndent()
        db.update(
            sql, mapOf(
                "oppfolgingsperiodeId" to oppfolgingsperiodeId.toString(),
                "avsluttesAutomatiskDato" to Timestamp.valueOf(avsluttesAutomatiskDato),
            )
        )
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
            SELECT kfuh.*, kfu.avsluttes_automatisk_dato
            FROM kandidater_for_utmelding kfu
            JOIN kandidater_for_utmelding_hendelser kfuh ON kfu.siste_utmeldingshendelse_id = kfuh.utmeldingshendelse_id
            WHERE kfu.oppfolgingsperiode_uuid = :oppfolgingsperiodeId AND kfu.forlenget_til IS NULL
            """.trimIndent(),
            mapOf("oppfolgingsperiodeId" to oppfolgingsperiodeId.toString()),
        ) { rs, _ -> map(rs) }
            .firstOrNull()
    }

    @TestOnly
    fun hentKandidatMedForlengelse(oppfolgingsperiodeId: UUID): KandidatForUtmeldingHendelse? {
        return db.query(
            """
            SELECT kfuh.*, kfu.avsluttes_automatisk_dato
            FROM kandidater_for_utmelding kfu
            JOIN kandidater_for_utmelding_hendelser kfuh ON kfu.siste_utmeldingshendelse_id = kfuh.utmeldingshendelse_id
            WHERE kfu.oppfolgingsperiode_uuid = :oppfolgingsperiodeId AND kfu.forlenget_til IS NOT NULL
            """.trimIndent(),
            mapOf("oppfolgingsperiodeId" to oppfolgingsperiodeId.toString()),
        ) { rs, _ -> map(rs) }
            .firstOrNull()
    }

    fun hentSisteHendelseForAktivKandidat(oppfolgingsperiodeId: UUID): KandidatForUtmeldingHendelse? {
        return db.query(
            """
            SELECT kfuh.*, kfu.avsluttes_automatisk_dato
            FROM kandidater_for_utmelding kfu
            JOIN kandidater_for_utmelding_hendelser kfuh ON kfu.siste_utmeldingshendelse_id = kfuh.utmeldingshendelse_id
            WHERE kfu.oppfolgingsperiode_uuid = :oppfolgingsperiodeId
            """.trimIndent(),
            mapOf("oppfolgingsperiodeId" to oppfolgingsperiodeId.toString()),
        ) { rs, _ -> map(rs) }
            .firstOrNull()
    }

    @TestOnly
    fun hentForlengetTil(oppfolgingsperiodeId: UUID): Timestamp? {
        return db.query(
            """
            SELECT forlenget_til
            FROM kandidater_for_utmelding
            WHERE oppfolgingsperiode_uuid = :oppfolgingsperiodeId
            """.trimIndent(),
            mapOf("oppfolgingsperiodeId" to oppfolgingsperiodeId.toString()),
        ) { rs, _ -> rs.getTimestamp("forlenget_til") }.firstOrNull()
    }

    @TestOnly
    fun hentAvsluttesAutomatiskDato(oppfolgingsperiodeId: UUID): Timestamp? {
        return db.query(
            """
            SELECT avsluttes_automatisk_dato
            FROM kandidater_for_utmelding
            WHERE oppfolgingsperiode_uuid = :oppfolgingsperiodeId
            """.trimIndent(),
            mapOf("oppfolgingsperiodeId" to oppfolgingsperiodeId.toString()),
        ) { rs, _ -> rs.getTimestamp("avsluttes_automatisk_dato") }.firstOrNull()
    }

    fun hentSisteKandidatForUtmeldingHendelse(oppfolgingsperiodeId: UUID): KandidatForUtmeldingHendelse? {
        return db.query(
            """
            SELECT kfuh.*, kfu.avsluttes_automatisk_dato
            FROM kandidater_for_utmelding_hendelser kfuh
            LEFT JOIN kandidater_for_utmelding kfu ON kfu.oppfolgingsperiode_uuid = kfuh.oppfolgingsperiode_uuid
            WHERE kfuh.oppfolgingsperiode_uuid = :oppfolgingsperiodeId order by kfuh.created_at desc
            LIMIT 1
            """.trimIndent(),
            mapOf("oppfolgingsperiodeId" to oppfolgingsperiodeId.toString()),
        ) { rs, _ -> map(rs) }
            .firstOrNull()
    }

    fun hentAktiveKandidater(offset: Int, batchSize: Int): List<KandidatForUtmeldingHendelse> {
        return db.query(
            """
            SELECT kfuh.*, kfu.avsluttes_automatisk_dato
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
            SELECT kfuh.*, kfu.avsluttes_automatisk_dato
            FROM kandidater_for_utmelding kfu
            JOIN kandidater_for_utmelding_hendelser kfuh ON kfu.siste_utmeldingshendelse_id = kfuh.utmeldingshendelse_id
            WHERE kfu.forlenget_til IS NOT NULL AND kfu.forlenget_til < current_timestamp
            """.trimIndent(),
        ) { rs, _ -> map(rs) }
    }

    fun hentAntallKandidaterForUtmelding(): Int {
        val sql = """
            SELECT COUNT(*) AS antall
            FROM kandidater_for_utmelding
        """.trimIndent()
        return db.queryForObject(sql, emptyMap<String, Any>()) { rs, _ -> rs.getInt("antall") }
    }

    fun hentAntallKandidaterForUtmeldingForlenget(): Int {
        val sql = """
            SELECT COUNT(*) AS antall
            FROM kandidater_for_utmelding
            WHERE forlenget_til IS NOT NULL
        """.trimIndent()
        return db.queryForObject(sql, emptyMap<String, Any>()) { rs, _ -> rs.getInt("antall") }
    }

    fun hentAntallKandidaterForUtmeldingIkkeForlenget(): Int {
        val sql = """
            SELECT COUNT(*) AS antall
            FROM kandidater_for_utmelding
            WHERE forlenget_til IS NULL
        """.trimIndent()
        return db.queryForObject(sql, emptyMap<String, Any>()) { rs, _ -> rs.getInt("antall") }
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
        val hendelsetype = resultSet.getString("hendelse")
        return when (getEnumType(hendelsetype)) {
            is ArbeidssokerperiodeAvsluttetHendelseType -> resultSet.toArbeidssøkerPeriodeAvsluttet()
            is ForlengelseHendelseType -> resultSet.toForlengelseHendelse()
        }
    }

    fun getEnumType(hendelse: String) : KandidatForUtmeldingHendelseType {
        return when (hendelse) {
            in ArbeidssokerperiodeAvsluttetHendelseType.entries.map { it.name } -> ArbeidssokerperiodeAvsluttetHendelseType.valueOf(hendelse)
            in ForlengelseHendelseType.entries.map { it.name } -> ForlengelseHendelseType.valueOf(hendelse)
            else -> {
                throw IllegalArgumentException("Ugyldig hendelse type: $hendelse")
            }
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
    arbeidssokerperiodeAvsluttetHendelseType = ArbeidssokerperiodeAvsluttetHendelseType.valueOf(getString("hendelse")),
    avsluttesAutomatiskDato = getTimestamp("avsluttes_automatisk_dato")?.toLocalDateTime(),
)

fun ResultSet.toForlengelseHendelse() = ForlengelseHendelse(
    oppfolgingsperiodeUuid = UUID.fromString(getString("oppfolgingsperiode_uuid")),
    utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.valueOf(getString("utfort_av_type")),
    utfortAv = getString("utfort_av"),
    kilde = getString("kilde"),
    hendelseTidspunkt = getTimestamp("hendelse_tidspunkt").toLocalDateTime().toInstant(ZoneOffset.UTC),
    forlengelseHendelseType = ForlengelseHendelseType.valueOf(getString("hendelse")),
    forlengetTil = getStringOrNull("hendelse_data")?.let { JsonUtils.fromJson(it, ForlengelseHendelse.Detaljer::class.java).forlengetTil },
    avsluttesAutomatiskDato = getTimestamp("avsluttes_automatisk_dato")?.toLocalDateTime(),
)