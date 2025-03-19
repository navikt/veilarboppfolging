package no.nav.veilarboppfolging.eventsLogger

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
open class AntallRegistrertInngarDAO(
    val template: JdbcTemplate
) {
    open fun hentAntallRegistrertMedInngar(): Int {
        val sql = """
            select count(*) from oppfolgingsperiode where start_begrunnelse = 'MANUELL_REGISTRERING_VEILEDER' and startdato > now() - INTERVAL '1 days'
        """.trimIndent()
        return template.queryForObject(sql, Int::class.java) ?: 0
    }
}