package no.nav.veilarboppfolging.kafka

import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.veilarboppfolging.domain.Oppfolgingsbruker
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.service.OppfolgingService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId

val DA_VI_STARTET_KONSUMERING = LocalDateTime.of(2024, 8, 1, 10, 0)
    .atZone(ZoneId.systemDefault())

        @Service
open class ArbeidssøkerperiodeConsumerService(
    @Lazy
    private val oppfolgingService: OppfolgingService,
    private val authService: AuthService
) {


    private val logger = LoggerFactory.getLogger(this::class.java)

    open fun consumeArbeidssøkerperiode(kafkaMelding: ConsumerRecord<String, Periode>) {
        val arbeidssøkerperiode: Periode = kafkaMelding.value()

        val arbeidssøkerperiodeStartet = arbeidssøkerperiode.startet.tidspunkt.atZone(ZoneId.systemDefault())
        if (arbeidssøkerperiodeStartet.isBefore(DA_VI_STARTET_KONSUMERING)) {
            return
        }

        val fnr = Fnr.of(arbeidssøkerperiode.identitetsnummer.toString())
        val aktørId = authService.getAktorIdOrThrow(fnr)

        val nyPeriode = arbeidssøkerperiode.avsluttet == null

        val nyestePeriodeStartDato = oppfolgingService.hentOppfolgingsperioder(aktørId)
            .maxByOrNull { it.startDato }



        if (nyPeriode) {
            val arbeidssøker = Oppfolgingsbruker.arbeidssokerOppfolgingsBruker(aktørId, null)
            logger.info("Fått melding om ny arbeidssøkerperiode, starter oppfølging hvis ikke allerede startet")
            oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(arbeidssøker)
        } else {
            logger.info("Melding om avsluttet oppfølgingsperiode, gjør ingenting")
        }
    }
}
