package no.nav.veilarboppfolging.service

import no.nav.common.types.identer.AktorId
import no.nav.veilarboppfolging.config.ApplicationConfig.SYSTEM_USER_NAME
import no.nav.veilarboppfolging.oppfolgingsbruker.AvsluttetAvType
import no.nav.veilarboppfolging.oppfolgingsbruker.StartetAvType
import no.nav.veilarboppfolging.oppfolgingsperioderHendelser.hendelser.OppfolgingStartetHendelseDto
import no.nav.veilarboppfolging.oppfolgingsperioderHendelser.hendelser.OppfolgingsAvsluttetHendelseDto
import no.nav.veilarboppfolging.repository.ArbeidsoppfolgingskontorRepository
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.utils.OppfolgingsperiodeUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class RepubliserOppfolgingshendelseService(
    private val authService: AuthService,
    private val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository,
    private val kafkaProducerService: KafkaProducerService,
    private val arbeidsoppfolgingskontorRepository: ArbeidsoppfolgingskontorRepository,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun republiserOppfolgingshendelseForBruker(aktorId: AktorId) {
        val fnr = authService.getFnrOrThrow(aktorId)
        val perioder = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId)

        if (perioder.isEmpty()) {
            log.warn("Fant ingen oppfølgingsperioder for bruker – republiserer ingenting")
            return
        }

        val sistePeriode = OppfolgingsperiodeUtils.hentSisteOppfolgingsperiode(perioder)

        if (sistePeriode.sluttDato == null) {
            log.info("Bruker er under oppfølging – republiserer OppfolgingStartet på oppfolgingshendelse-topic for periode ${sistePeriode.uuid}")
            val kontor = arbeidsoppfolgingskontorRepository.hentEnhet(aktorId)?.get()
            val hendelse = OppfolgingStartetHendelseDto(
                oppfolgingsPeriodeId = sistePeriode.uuid,
                startetTidspunkt = sistePeriode.startDato,
                startetAv = sistePeriode.startetAv
                    ?: throw IllegalStateException("Kan ikke republisere OppfolgingStartet: mangler 'startetAv' på periode ${sistePeriode.uuid}"),
                startetAvType = sistePeriode.startetAvType
                    ?: throw IllegalStateException("Kan ikke republisere OppfolgingStartet: mangler 'startetAvType' på periode ${sistePeriode.uuid}"),
                startetBegrunnelse = sistePeriode.startetBegrunnelse
                    ?: throw IllegalStateException("Kan ikke republisere OppfolgingStartet: mangler 'startetBegrunnelse' på periode ${sistePeriode.uuid}"),
                foretrukketArbeidsoppfolgingskontor = kontor,
                fnr = fnr.get(),
            )
            kafkaProducerService.publiserOppfolgingsStartet(hendelse)
        } else {
            log.info("Bruker er ikke under oppfølging – republiserer OppfolgingAvsluttet på oppfolgingshendelse-topic for periode ${sistePeriode.uuid}")
            val avsluttetAv = sistePeriode.avsluttetAv ?: SYSTEM_USER_NAME
            val avsluttetAvType: AvsluttetAvType =
                if (avsluttetAv == SYSTEM_USER_NAME) StartetAvType.SYSTEM else StartetAvType.VEILEDER
            val avregistreringsType = sistePeriode.avregistreringsType
                ?: throw IllegalStateException("Kan ikke republisere OppfolgingAvsluttet: mangler 'avregistreringsType' på periode ${sistePeriode.uuid}")

            val hendelse = OppfolgingsAvsluttetHendelseDto(
                fnr = fnr.get(),
                oppfolgingsPeriodeId = sistePeriode.uuid,
                startetTidspunkt = sistePeriode.startDato,
                avsluttetTidspunkt = sistePeriode.sluttDato,
                avsluttetAv = avsluttetAv,
                avsluttetAvType = avsluttetAvType,
                avregistreringsType = avregistreringsType,
            )
            kafkaProducerService.publiserOppfolgingsAvsluttet(hendelse)
        }
    }
}
