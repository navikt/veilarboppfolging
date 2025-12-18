package no.nav.veilarboppfolging.eventsLogger

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class KontorMetrikkerDAO(val db: NamedParameterJdbcTemplate) {

    fun hentAvvikendeArenaOgAoKontor(): List<ArenakontorUtenAoKontor> {
        val sql = """
            select kontor_id as ao_kontor, oppfolgingsenhet as arena_kontor, oppfolgingsperiode_id, startdato, o.oppdatert from ao_kontor
            left join oppfolgingsperiode on ao_kontor.oppfolgingsperiode_id = oppfolgingsperiode.uuid::uuid
            left join oppfolgingstatus o on o.aktor_id = ao_kontor.aktor_id
            where o.under_oppfolging = 1 and ao_kontor.kontor_id != o.oppfolgingsenhet
        """.trimIndent()

        return db.query(sql) { rs, _ ->
            ArenakontorUtenAoKontor(
                oppfolgingsperiodeId = rs.getString("oppfolgingsperiode_id"),
                arenaKontor = rs.getString("arena_kontor"),
                aoKontor = rs.getString("ao_kontor"),
            )
        }
    }

    fun hentOppfolgingsperioderUtenAoKontor(): List<OppfolgingsperiodeUtenAoKontor> {
        val sql = """
            select uuid from oppfolgingsperiode b
            left join ao_kontor a on a.oppfolgingsperiode_id = b.uuid::uuid
            where b.sluttdato is null and a.oppfolgingsperiode_id is null
        """.trimIndent()

        return db.query(sql) { rs, _ ->
            OppfolgingsperiodeUtenAoKontor(
                oppfolgingsperiodeId = rs.getString("uuid"),
            )
        }
    }
}