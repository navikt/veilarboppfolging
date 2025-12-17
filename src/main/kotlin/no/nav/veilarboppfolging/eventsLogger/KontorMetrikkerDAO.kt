package no.nav.veilarboppfolging.eventsLogger

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

class KontorMetrikkerDAO(val db: NamedParameterJdbcTemplate) {

    fun hentAvvikendeArenaOgAoKontor() : List<ArenakontorUtenAoKontor> {
        val sql = """
            select kontor_id as ao_kontor, oppfolgingsenhet as arena_kontor, ident, o.aktor_id, oppfolgingsperiode_id, startdato, o.oppdatert from ao_kontor
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
}