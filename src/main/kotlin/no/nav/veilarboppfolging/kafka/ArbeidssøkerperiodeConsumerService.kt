package no.nav.veilarboppfolging.kafka

import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.veilarboppfolging.domain.StartetAvType
import no.nav.veilarboppfolging.oppfolgingsbruker.Oppfolgingsbruker
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.service.IservService
import no.nav.veilarboppfolging.service.OppfolgingService
import no.nav.veilarboppfolging.service.utmelding.KanskjeIservBruker
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

val DA_VI_STARTET_KONSUMERING = LocalDateTime.of(2024, 8, 6, 0, 0)
    .atZone(ZoneId.systemDefault())

        @Service
open class ArbeidssøkerperiodeConsumerService(
            @Lazy
    private val oppfolgingService: OppfolgingService,
            private val authService: AuthService,
            private val arenaOppfolgingService: ArenaOppfolgingService,
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
            val startetAvType = arbeidssøkerperiode.startet.utfoertAv.type // VEILEDER, SYSTEM, SLUTTBRUKER
            // TODO: Når vi fjerner /aktiverbruker endepunkt bør vi også fjerne innsatsgruppe-feltet på Oppfolgingsbruker
            val arbeidssøker = Oppfolgingsbruker.arbeidssokerOppfolgingsBruker(aktørId, startetAvType.toStartetAvType())
            logger.info("Fått melding om ny arbeidssøkerperiode, starter oppfølging hvis ikke allerede startet")
            oppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(arbeidssøker)
            utmeldHvisAlleredeIserv(fnr, arbeidssøkerperiodeStartet)
        } else {
            logger.info("Melding om avsluttet oppfølgingsperiode, gjør ingenting")
        }
    }

    private fun BrukerType.toStartetAvType(): StartetAvType {
        return when (this) {
            BrukerType.UKJENT_VERDI -> StartetAvType.SYSTEM
            BrukerType.UDEFINERT -> StartetAvType.SYSTEM
            BrukerType.VEILEDER -> StartetAvType.VEILEDER
            BrukerType.SYSTEM -> StartetAvType.SYSTEM
            BrukerType.SLUTTBRUKER -> StartetAvType.BRUKER
        }
    }

    fun utmeldHvisAlleredeIserv(fnr: Fnr, arbeidssøkerperiodeStartet: ZonedDateTime) {
        runCatching {
            val oppfolgingsbruker = arenaOppfolgingService.hentIservDatoOgFormidlingsGruppe(fnr) ?: throw IllegalStateException("Fant ikke bruker")
            if (oppfolgingsbruker.iservDato == null || oppfolgingsbruker.formidlingsGruppe == null) return@runCatching null
            KanskjeIservBrukerMedPresisIservDato(oppfolgingsbruker.iservDato, fnr.get(), oppfolgingsbruker.formidlingsGruppe)
        }.onSuccess { kanskjeIservBruker ->
            if (kanskjeIservBruker == null) return
            if (kanskjeIservBruker.iservFraDato.atStartOfDay(ZoneId.systemDefault()).isAfter(arbeidssøkerperiodeStartet)) {
                logger.info("Bruker ble ${kanskjeIservBruker.formidlingsgruppe} etter arbeidssøkerregistrering, sjekker om bruker bør utmeldes")
                iservService.oppdaterUtmeldingsStatus(kanskjeIservBruker.toKanskjeIservBruker())
            }
        }.onFailure { logger.warn("Kunne ikke hente oppfolgingsstatus (arena) for bruker under prosessering av arbeidssøkerregistrering, sjekker ikke om bruker skal i utmelding", it) }
    }
}

data class KanskjeIservBrukerMedPresisIservDato(
    val iservFraDato: LocalDate,
    val fnr: String,
    val formidlingsgruppe: Formidlingsgruppe
) {
    fun toKanskjeIservBruker(): KanskjeIservBruker = KanskjeIservBruker(this.iservFraDato, this.fnr, this.formidlingsgruppe)
}
