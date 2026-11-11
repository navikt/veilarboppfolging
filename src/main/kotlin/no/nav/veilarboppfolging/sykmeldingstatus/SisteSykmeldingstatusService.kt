package no.nav.veilarboppfolging.sykmeldingstatus

import java.util.UUID
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.client.isoppfolgingstilfelle.IsOppfolgingstilfelleClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SisteSykmeldingstatusService(
    private val sisteSykmeldingstatusRepository: SisteSykmeldingstatusRepository,
    private val isOppfolgingstilfelleClient: IsOppfolgingstilfelleClient,
) {
    val log = LoggerFactory.getLogger(this::class.java)

    fun hentOgLagreSisteSykmeldingstatus(oppfolgingsperiodeId: UUID, fnr: Fnr) {
        log.info("Henter siste oppfolgingstilfelle fra isyfo for oppfølgingsperiode $oppfolgingsperiodeId")
        val sisteOppfolgingstilfelle = isOppfolgingstilfelleClient.hentAktivtOppfolgingstilfelle(fnr.get())

        if (sisteOppfolgingstilfelle != null) {
            val sisteSykmeldingstatus = SisteSykmeldingstatus(
                oppfolgingsperiodeId = oppfolgingsperiodeId,
                fnr = fnr.get(),
                startdato = sisteOppfolgingstilfelle.start,
                sluttdato = sisteOppfolgingstilfelle.end,
                harArbeidsgiver = sisteOppfolgingstilfelle.arbeidstakerAtTilfelleEnd,
            )
            sisteSykmeldingstatusRepository.lagreSisteSykmeldingstatus(sisteSykmeldingstatus)
            log.info("Oppdatert siste sykmeldingstatus for oppfølgingsperiode $oppfolgingsperiodeId")
        }
    }
}