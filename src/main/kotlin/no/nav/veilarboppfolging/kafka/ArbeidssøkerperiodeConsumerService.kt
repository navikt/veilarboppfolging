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
import org.springframework.transaction.annotation.Transactional
import java.lang.IllegalStateException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.jvm.optionals.getOrElse

val DA_VI_STARTET_KONSUMERING = LocalDateTime.of(2024, 8, 6, 6, 0)
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

    @Transactional
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
            // TODO: Når vi fjerner /aktiverbruker endepunkt bør vi også fjerne innsatsgruppe-feltet på Oppfolgingsbruker
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
            val oppfolgingsbruker = veilarbarenaClient.hentOppfolgingsbruker(fnr)
                .getOrElse { throw IllegalStateException("Fant ikke bruker") }
            if (oppfolgingsbruker.iserv_fra_dato == null || oppfolgingsbruker.formidlingsgruppekode == null) return@runCatching null
            KanskjeIservBrukerMedPresisIserbDato(oppfolgingsbruker.iserv_fra_dato, fnr.get(), Formidlingsgruppe.valueOf(oppfolgingsbruker.formidlingsgruppekode))
        }.onSuccess { kanskjeIservBruker ->
            if (kanskjeIservBruker == null) return
            if (kanskjeIservBruker.iservFraDato.isAfter(arbeidssøkerperiodeStartet)) {
                logger.info("Bruker ble ${kanskjeIservBruker.formidlingsgruppe} etter arbeidssøkerregistrering, sjekker om bruker bør utmeldes")
                iservService.oppdaterUtmeldingsStatus(kanskjeIservBruker.toKanskjeIservBruker())
            }
        }.onFailure { logger.error("Kunne ikke hente oppfolgingsstatus for bruker", it) }
    }
}

data class KanskjeIservBrukerMedPresisIserbDato(
    val iservFraDato: ZonedDateTime,
    val fnr: String,
    val formidlingsgruppe: Formidlingsgruppe
) {
    fun toKanskjeIservBruker(): KanskjeIservBruker = KanskjeIservBruker(this.iservFraDato.toLocalDate(), this.fnr, this.formidlingsgruppe)
}
