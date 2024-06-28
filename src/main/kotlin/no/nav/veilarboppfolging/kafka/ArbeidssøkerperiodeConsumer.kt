package no.nav.veilarboppfolging.kafka

import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.veilarboppfolging.domain.Oppfolgingsbruker
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.service.OppfolgingService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ArbeidssøkerperiodeConsumer(
    private val oppfolgingService: OppfolgingService,
    private val authService: AuthService
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun consumeArbeidssøkerperiode(kafkaMelding: ConsumerRecord<String, Periode>) {
        val arbeidssøkerperiode = kafkaMelding.value()
        val fnr = Fnr.of(arbeidssøkerperiode.getIdentitetsnummer().toString())
        val aktørId = authService.getAktorIdOrThrow(fnr)

        val nyPeriode = arbeidssøkerperiode.getAvsluttet() == null

        if (nyPeriode) {
            val arbeidssøker = Oppfolgingsbruker.nyttArbeidssøkerregisterBruker(aktørId)
            oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(arbeidssøker, fnr)
            logger.info("Startet oppfølgingsperiode basert på ny arbeidssøkerperiode")
        }
    }
}
