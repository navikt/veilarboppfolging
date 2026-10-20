package no.nav.veilarboppfolging.kafka

import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.AvsluttetAarsakType
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.veilarboppfolging.arbeidssokerperiode.AktivArbeidssokerperiodeRepository
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingService
import no.nav.veilarboppfolging.kandidatForUtmelding.hendelser.ArbeidssokerperiodeAvsluttetHendelseType
import no.nav.veilarboppfolging.kandidatForUtmelding.hendelser.ArbeidssøkerPeriodeAvsluttet
import no.nav.veilarboppfolging.kandidatForUtmelding.hendelser.KandidatForUtmeldingHendelseUtfortAvType
import no.nav.veilarboppfolging.service.AuthService
import no.nav.veilarboppfolging.service.OppfolgingService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
open class MidlertidigArbeidssokerperiodeConsumer(
    @Lazy
    private val oppfolgingService: OppfolgingService,
    private val authService: AuthService,
    private val kandidatForUtmeldingService: KandidatForUtmeldingService,
    private val aktivArbeidssokerperiodeRepository: AktivArbeidssokerperiodeRepository,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    open fun consumeArbeidssokerperiode(kafkaMelding: ConsumerRecord<String, Periode>) {
        val arbeidssøkerperiode: Periode = kafkaMelding.value()
        val arbeidssokerperiodeId = arbeidssøkerperiode.id.toString()

        val fnr = Fnr.of(arbeidssøkerperiode.identitetsnummer.toString())
        val aktørId = authService.getAktorIdOrThrow(fnr)

        val oppfolgingsperioder = oppfolgingService.hentOppfolgingsperioder(aktørId)

        val nyPeriode = arbeidssøkerperiode.avsluttet == null
        if (nyPeriode) {
            logger.info("Ignorerer ny arbeidssøkerperiode $arbeidssokerperiodeId")
            return
        } else {
            logger.info("Melding om avsluttet arbeidssøkerperiode $arbeidssokerperiodeId, flagger som utmeldingskandidat hvis under oppfølging")
            aktivArbeidssokerperiodeRepository.slettAktivArbeidssokerperiode(arbeidssøkerperiode.id.toString())
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
