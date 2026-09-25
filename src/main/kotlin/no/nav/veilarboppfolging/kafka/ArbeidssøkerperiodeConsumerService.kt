package no.nav.veilarboppfolging.kafka

import java.time.LocalDateTime
import java.time.ZoneId
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.paw.arbeidssokerregisteret.api.v1.AvsluttetAarsakType
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.veilarboppfolging.kandidatForUtmelding.hendelser.ArbeidssokerperiodeAvsluttetHendelseType
import no.nav.veilarboppfolging.kandidatForUtmelding.hendelser.ArbeidssøkerPeriodeAvsluttet
import no.nav.veilarboppfolging.kandidatForUtmelding.hendelser.KandidatForUtmeldingHendelseUtfortAvType
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingService
import no.nav.veilarboppfolging.oppfolgingsbruker.StartetAvType
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering
import no.nav.veilarboppfolging.oppfolgingsbruker.toRegistrant
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.service.OppfolgingService
import no.nav.veilarboppfolging.service.StartOppfolgingService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

val DA_VI_STARTET_KONSUMERING = LocalDateTime.of(2024, 8, 6, 0, 0)
    .atZone(ZoneId.systemDefault())

        @Service
open class ArbeidssøkerperiodeConsumerService(
            @Lazy
            private val oppfolgingService: OppfolgingService,
            private val startOppfolgingService: StartOppfolgingService,
            private val authService: AuthService,
            private val kandidatForUtmeldingService: KandidatForUtmeldingService,
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
            val nyestePeriode = oppfolgingsperioder.maxByOrNull { it.startDato }
            val nyestePeriodeStartDato = nyestePeriode?.startDato
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
        } else {
            logger.info("Melding om avsluttet arbeidssøkerperiode, flagger som utmeldingskandidat hvis under oppfølging")
            val gjeldendePeriode = oppfolgingsperioder.firstOrNull { it.sluttDato == null }
            if (gjeldendePeriode != null) {
                val kilde = arbeidssøkerperiode.avsluttet?.kilde ?: "arbeidssøkerregisteret"
                val avsluttetAarsakType = arbeidssøkerperiode.avslutningsInfo?.aarsaksinformasjon?.type
                val avsluttetAv = when(arbeidssøkerperiode.avsluttet?.utfoertAv?.type) {
                    BrukerType.UKJENT_VERDI, BrukerType.UDEFINERT, null -> KandidatForUtmeldingHendelseUtfortAvType.UKJENT
                    BrukerType.VEILEDER -> KandidatForUtmeldingHendelseUtfortAvType.VEILEDER
                    BrukerType.SYSTEM -> KandidatForUtmeldingHendelseUtfortAvType.SYSTEM
                    BrukerType.SLUTTBRUKER -> KandidatForUtmeldingHendelseUtfortAvType.BRUKER
                }
                val type = when (avsluttetAarsakType) {
                    AvsluttetAarsakType.SVARTE_NEI_I_BEKREFTELSE -> ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE
                    AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST -> ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT
                    AvsluttetAarsakType.UDEFINERT, AvsluttetAarsakType.UKJENT_VERDI, null -> ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET
                }
                kandidatForUtmeldingService.handterUtmeldingsHendelse(
                    fnr,
                    ArbeidssøkerPeriodeAvsluttet(
                        oppfolgingsperiodeUuid = gjeldendePeriode.uuid,
                        utfortAvType = avsluttetAv,
                        utfortAv = arbeidssøkerperiode.avsluttet?.utfoertAv?.id,
                        kilde = kilde,
                        avslutningsarsak = avsluttetAarsakType?.toString(),
                        hendelseTidspunkt = arbeidssøkerperiode.avsluttet.tidspunkt,
                        arbeidssokerperiodeAvsluttetHendelseType = type,
                    )
                )
            }
        }
    }
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