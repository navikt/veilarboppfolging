package no.nav.veilarboppfolging.service

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicLong

@Service
class ArbeidsoppfolgingsKontorAlertService(
    private val jdbcTemplate: JdbcTemplate,
    meterRegistry: MeterRegistry
) {
    private val antallÅpneOppfølgingperioderUtenAoKontor = AtomicLong(0)

    init {
        meterRegistry.gauge("veilarboppfolging_oppfolgingsperioder_uten_ao_kontor", antallÅpneOppfølgingperioderUtenAoKontor)
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000, initialDelay = 60 * 1000) // hvert 5. min
    fun målAntallÅpneOppfølgingsperioderUtenAoKontor() {
        val antall = jdbcTemplate.queryForObject("""
            select count(*) from oppfolgingsperiode
            left join ao_kontor on oppfolgingsperiode.uuid::uuid = ao_kontor.oppfolgingsperiode_id
            where oppfolgingsperiode.sluttdato is null
              and oppfolgingsperiode.startdato < now() - interval '20 minutes'
              and ao_kontor.oppfolgingsperiode_id is null;
        """.trimIndent()
        , Long::class.java) ?: 0L
        antallÅpneOppfølgingperioderUtenAoKontor.set(antall)
    }
}
