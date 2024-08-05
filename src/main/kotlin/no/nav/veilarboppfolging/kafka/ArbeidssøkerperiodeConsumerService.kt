package no.nav.veilarboppfolging.kafka

import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.veilarboppfolging.client.veilarbarena.VeilarbarenaClient
import no.nav.veilarboppfolging.domain.Oppfolgingsbruker
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.service.IservService
import no.nav.veilarboppfolging.service.OppfolgingService
import no.nav.veilarboppfolging.service.utmelding.KanskjeIservBruker
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import java.lang.IllegalStateException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.jvm.optionals.getOrNull

val DA_VI_STARTET_KONSUMERING = LocalDateTime.of(2024, 8, 5, 10, 0)
    .atZone(ZoneId.systemDefault())

        @Service
open class ArbeidssøkerperiodeConsumerService(
            @Lazy
    private val oppfolgingService: OppfolgingService,
            private val authService: AuthService,
            private val veilarbarenaClient: VeilarbarenaClient,
            private val iservService: IservService,
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

        val nyestePeriodeStartDato = oppfolgingService.hentOppfolgingsperioder(aktørId)
            .maxByOrNull { it.startDato }?.startDato
        if (nyestePeriodeStartDato?.isAfter(arbeidssøkerperiodeStartet) == true) {
            logger.info("Har allerede registrert oppfølgingsperiode etter startdato for arbeidssøkerperiode")
            return
        }

        val nyPeriode = arbeidssøkerperiode.avsluttet == null
        if (nyPeriode) {
            val arbeidssøker = Oppfolgingsbruker.arbeidssokerOppfolgingsBruker(aktørId, null)
            logger.info("Fått melding om ny arbeidssøkerperiode, starter oppfølging hvis ikke allerede startet")
            oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(arbeidssøker)
            utmeldHvisAlleredeIserv(fnr, arbeidssøkerperiodeStartet)
        } else {
            logger.info("Melding om avsluttet oppfølgingsperiode, gjør ingenting")
        }
    }

    fun utmeldHvisAlleredeIserv(fnr: Fnr, arbeidssøkerperiodeStartet: ZonedDateTime) {
        runCatching {
            veilarbarenaClient.hentOppfolgingsbruker(fnr).getOrNull()
                ?.let { it.iserv_fra_dato to it.formidlingsgruppekode }
                ?.also {
                    requireNotNull(it.first) { "iserv_fra_dato kan ikke være null" }
                    requireNotNull(it.second) { "formidlingsgruppekode kan ikke være null" }
                } ?: throw IllegalStateException("Fant ikke bruker")
        }.onSuccess { (iservFraDato, formidlingsgruppe) ->
            if (iservFraDato.isAfter(arbeidssøkerperiodeStartet)) {
                logger.info("Bruker ble $formidlingsgruppe etter arbeidssøkerregistrering, sjekker om bruker bør utmeldes")
                iservService.oppdaterUtmeldingsStatus(
                    KanskjeIservBruker(iservFraDato.toLocalDate(), fnr.get(), Formidlingsgruppe.valueOf(formidlingsgruppe))
                )
            }
        }.onFailure { logger.error("Kunne ikke hente oppfolgingsstatus for bruker", it) }
    }
}
