package no.nav.veilarboppfolging.kafka

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.veilarboppfolging.oppfolgingsbruker.StartetAvType
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering
import no.nav.veilarboppfolging.oppfolgingsbruker.toRegistrant
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.UtmeldingsService
import no.nav.veilarboppfolging.kandidatForUtmelding.ArbeidssøkerPeriodeAvsluttet
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingHendelseAvsluttetAv
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingService
import no.nav.veilarboppfolging.service.OppfolgingService
import no.nav.veilarboppfolging.service.StartOppfolgingService
import no.nav.veilarboppfolging.service.utmelding.IservTrigger
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
            private val startOppfolgingService: StartOppfolgingService,
            private val authService: AuthService,
            private val arenaOppfolgingService: ArenaOppfolgingService,
            private val utmeldingService: UtmeldingsService,
            private val kandidatForUtmeldingService: KandidatForUtmeldingService
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

        val oppfolgingsperioder = oppfolgingService.hentOppfolgingsperioder(aktørId)

        val nyPeriode = arbeidssøkerperiode.avsluttet == null
        if (nyPeriode) {
            val nyestePeriodeStartDato = oppfolgingsperioder.maxByOrNull { it.startDato }?.startDato
            if (nyestePeriodeStartDato?.isAfter(arbeidssøkerperiodeStartet) == true) {
                logger.info("Har allerede registrert oppfølgingsperiode etter startdato for arbeidssøkerperiode")
                return
            }
            val startetAvType = arbeidssøkerperiode.startet.utfoertAv.type
            // TODO: Når vi fjerner /aktiverbruker endepunkt bør vi også fjerne innsatsgruppe-feltet på Oppfolgingsbruker
            logger.info("Fått melding om ny arbeidssøkerperiode, starter oppfølging hvis ikke allerede startet")

            val navIdent = NavIdent.of(arbeidssøkerperiode.startet.utfoertAv.id.toString())
            val registrant =  startetAvType.toStartetAvType().toRegistrant(navIdent, fnr)

            startOppfolgingService.startOppfolgingHvisIkkeAlleredeStartet(OppfolgingsRegistrering.arbeidssokerRegistrering(fnr, aktørId, registrant))
            kandidatForUtmeldingService.fjernKandidatForUtmelding(aktørId)
            utmeldHvisBrukerBleIservEtterArbeidssøkerRegistrering(fnr, arbeidssøkerperiodeStartet, aktørId)
        } else {
            logger.info("Melding om avsluttet arbeidssøkerperiode, flagger som utmeldingskandidat hvis under oppfølging")
            if (oppfolgingsperioder.any { it.sluttDato == null }) {
                val kilde = arbeidssøkerperiode.avsluttet?.kilde?.toString() ?: "arbeidssøkerregisteret"
                val aarsak = arbeidssøkerperiode.avsluttet?.aarsak?.toString()
                val avsluttetAv = when(arbeidssøkerperiode.avsluttet?.utfoertAv?.type) {
                    BrukerType.UKJENT_VERDI, BrukerType.UDEFINERT, null -> KandidatForUtmeldingHendelseAvsluttetAv.UKJENT
                    BrukerType.VEILEDER -> KandidatForUtmeldingHendelseAvsluttetAv.VEILEDER
                    BrukerType.SYSTEM -> KandidatForUtmeldingHendelseAvsluttetAv.SYSTEM
                    BrukerType.SLUTTBRUKER -> KandidatForUtmeldingHendelseAvsluttetAv.BRUKER
                }
                kandidatForUtmeldingService.lagreKandidatForUtmelding(ArbeidssøkerPeriodeAvsluttet(aktørId, fnr, avsluttetAv = avsluttetAv, kilde = kilde, aarsak = aarsak))
            }
        }
    }

    fun utmeldHvisBrukerBleIservEtterArbeidssøkerRegistrering(fnr: Fnr, arbeidssøkerperiodeStartet: ZonedDateTime, aktorId: AktorId) {
        runCatching {
            val oppfolgingsbruker = arenaOppfolgingService.hentIservDatoOgFormidlingsGruppe(fnr) ?: throw IllegalStateException("Fant ikke bruker")
            if (oppfolgingsbruker.iservDato == null || oppfolgingsbruker.formidlingsGruppe == null) return@runCatching null
            KanskjeIservBrukerMedPresisIservDato(oppfolgingsbruker.iservDato, aktorId, oppfolgingsbruker.formidlingsGruppe)
        }.onSuccess { kanskjeIservBruker ->
            if (kanskjeIservBruker == null) return
            if (kanskjeIservBruker.iservFraDato.atStartOfDay(ZoneId.systemDefault()).isAfter(arbeidssøkerperiodeStartet)) {
                logger.info("Bruker ble ${kanskjeIservBruker.formidlingsgruppe} etter arbeidssøkerregistrering, sjekker om bruker bør utmeldes")
                utmeldingService.oppdaterUtmeldingsStatus(kanskjeIservBruker.toKanskjeIservBruker())
            }
        }.onFailure { logger.warn("Kunne ikke hente oppfolgingsstatus (arena) for bruker under prosessering av arbeidssøkerregistrering, sjekker ikke om bruker skal i utmelding", it) }
    }
}

data class KanskjeIservBrukerMedPresisIservDato(
    val iservFraDato: LocalDate,
    val aktorId: AktorId,
    val formidlingsgruppe: Formidlingsgruppe
) {
    fun toKanskjeIservBruker(): KanskjeIservBruker = KanskjeIservBruker(this.iservFraDato, this.aktorId, this.formidlingsgruppe, IservTrigger.ArbeidssøkerRegistreringSync)
}

fun BrukerType.toStartetAvType(): StartetAvType {
    return when (this) {
        BrukerType.UKJENT_VERDI -> StartetAvType.SYSTEM
        BrukerType.UDEFINERT -> StartetAvType.SYSTEM
        BrukerType.VEILEDER -> StartetAvType.VEILEDER
        BrukerType.SYSTEM -> StartetAvType.SYSTEM
        BrukerType.SLUTTBRUKER -> StartetAvType.BRUKER
    }
}