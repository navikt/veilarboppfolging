package no.nav.veilarboppfolging.sykmeldingstatus

import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class SisteSykmeldingstatusRepository(
    private val db: NamedParameterJdbcTemplate,
) {
    fun lagreSisteSykmeldingstatus(sisteSykmeldingstatus: SisteSykmeldingstatus) {
        val sql = """
            INSERT INTO siste_sykmeldingstatus(oppfolgingsperiode_uuid, fnr, startdato, sluttdato, har_arbeidsgiver)
            VALUES (:oppfolgingsperiodeId, :fnr, :startdato, :sluttdato, :harArbeidsgiver)
            ON CONFLICT (oppfolgingsperiode_uuid) 
            DO UPDATE SET updated_at = current_timestamp, fnr = :fnr, startdato = :startdato, sluttdato = :sluttdato, har_arbeidsgiver = :harArbeidsgiver
        """.trimIndent()
        db.update(
            sql, mapOf(
                "oppfolgingsperiodeId" to sisteSykmeldingstatus.oppfolgingsperiodeId.toString(),
                "fnr" to sisteSykmeldingstatus.fnr,
                "startdato" to sisteSykmeldingstatus.startdato,
                "sluttdato" to sisteSykmeldingstatus.sluttdato,
                "harArbeidsgiver" to sisteSykmeldingstatus.harArbeidsgiver,
            )
        )
    }

    fun slettSisteSykmeldingstatus(oppfolgingsperiodeId: UUID) {
        val sql = """
            DELETE FROM siste_sykmeldingstatus
            WHERE oppfolgingsperiode_uuid = :oppfolgingsperiodeId
        """.trimIndent()
        db.update(sql, mapOf("oppfolgingsperiodeId" to oppfolgingsperiodeId.toString()))
    }

    fun hentSisteSykmeldingstatus(oppfolgingsperiodeId: UUID): SisteSykmeldingstatus? {
        val sql = """
            SELECT * FROM siste_sykmeldingstatus
            WHERE oppfolgingsperiode_uuid = :oppfolgingsperiodeId
        """.trimIndent()
        return db.query(sql, mapOf("oppfolgingsperiodeId" to oppfolgingsperiodeId.toString())) { rs, _ ->
            rs.toSisteSykmeldingstatus()
        }.firstOrNull()
    }

    fun hentSisteSykmeldingstatusAvsluttetOgIkkeKandidat(avsluttetTidligereEnn: LocalDate): List<SisteSykmeldingstatus> {
        val sql = """
            SELECT * FROM siste_sykmeldingstatus ss
            left join kandidater_for_utmelding kfu on ss.oppfolgingsperiode_uuid = kfu.oppfolgingsperiode_uuid
            WHERE kfu.oppfolgingsperiode_uuid IS NULL
            AND ss.sluttdato < :avsluttetTidligereEnn
        """.trimIndent()
        return db.query(sql, mapOf("avsluttetTidligereEnn" to avsluttetTidligereEnn)) { rs, _ ->
            rs.toSisteSykmeldingstatus()
        }
    }

    fun ResultSet.toSisteSykmeldingstatus() = SisteSykmeldingstatus(
        oppfolgingsperiodeId = UUID.fromString(getString("oppfolgingsperiode_uuid")),
        fnr = getString("fnr"),
        startdato = getDate("startdato").toLocalDate(),
        sluttdato = getDate("sluttdato").toLocalDate(),
        harArbeidsgiver = getBoolean("har_arbeidsgiver"),
    )
}