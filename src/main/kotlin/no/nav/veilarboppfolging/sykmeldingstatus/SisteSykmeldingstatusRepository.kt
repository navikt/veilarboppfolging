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
            DO UPDATE SET updated_at = current_timestamp, fnr = :fnr, startdato = :startdato, sluttdato = :sluttdato, har_arbeidsgiver = :harArbeidsgiver, sist_sjekket_avslutningsstatus = NULL
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

    fun hentSykmeldteUtenArbeidsgiverMedStatusAvsluttetSomIkkeErUtmeldingsKandidater(avsluttetTidligereEnn: LocalDate): List<SisteSykmeldingstatus> {
        val sql = """
            SELECT * FROM siste_sykmeldingstatus ss
            left join kandidater_for_utmelding kfu on ss.oppfolgingsperiode_uuid = kfu.oppfolgingsperiode_uuid
            WHERE kfu.oppfolgingsperiode_uuid IS NULL
            AND ss.sluttdato < :avsluttetTidligereEnn
            AND ss.sist_sjekket_avslutningsstatus IS NULL 
            AND ss.har_arbeidsgiver = false
            AND NOT EXISTS (
                SELECT 1 FROM kandidater_som_ikke_kunne_avsluttes ksika
                WHERE ksika.oppfolgingsperiode_uuid = ss.oppfolgingsperiode_uuid
            )
        """.trimIndent()
        return db.query(sql, mapOf("avsluttetTidligereEnn" to avsluttetTidligereEnn)) { rs, _ ->
            rs.toSisteSykmeldingstatus()
        }
    }

    fun oppdaterSistSjekketAvslutningsstatus(oppfolgingsperiodeId: UUID) {
        val sql = """
            UPDATE siste_sykmeldingstatus
            SET sist_sjekket_avslutningsstatus = current_timestamp
            WHERE oppfolgingsperiode_uuid = :oppfolgingsperiodeId
        """.trimIndent()
        db.update(sql, mapOf("oppfolgingsperiodeId" to oppfolgingsperiodeId.toString()))
    }

    fun ResultSet.toSisteSykmeldingstatus() = SisteSykmeldingstatus(
        oppfolgingsperiodeId = UUID.fromString(getString("oppfolgingsperiode_uuid")),
        fnr = getString("fnr"),
        startdato = getDate("startdato").toLocalDate(),
        sluttdato = getDate("sluttdato").toLocalDate(),
        harArbeidsgiver = getBoolean("har_arbeidsgiver"),
    )
}