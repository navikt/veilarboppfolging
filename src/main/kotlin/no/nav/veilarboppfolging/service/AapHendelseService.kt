package no.nav.veilarboppfolging.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.client.aap.AapClient
import no.nav.veilarboppfolging.kafka.AapHendelseRecord
import no.nav.veilarboppfolging.kandidatForUtmelding.FjernKandidatForUtmeldingService
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingRepository
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingService
import no.nav.veilarboppfolging.kandidatForUtmelding.hendelser.AvsluttetAapSak
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import kotlin.jvm.optionals.getOrNull

@Service
class AapHendelseService(
    val aapClient: AapClient,
    val kandidatForUtmeldingService: KandidatForUtmeldingService,
    val aktorOppslagClient: AktorOppslagClient,
    val fjernKandidatForUtmeldingService: FjernKandidatForUtmeldingService,
    val kandidatForUtmeldingRepository: KandidatForUtmeldingRepository,
    private val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository
) {
    val log = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun håndterAapHendelse(key: String, value: AapHendelseRecord) {
        log.info("Mottatt AAP hendelse: ${value.hendelse}")
        val fnr = Fnr.of(key)
        val aktorId = aktorOppslagClient.hentAktorId(fnr)
        val oppfolgingsperiode = oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId)
            .getOrNull()?.uuid
        if (oppfolgingsperiode == null) {
            log.info("Ignorerer AAP-hendelse: bruker har ingen gjeldende oppfolgingsperiode")
            return
        }

        val harSoktEllerMottarAap = aapClient.harAap(fnr.get())
        kandidatForUtmeldingRepository.fjernKandidatSomIkkeKunneAvsluttes(oppfolgingsperiode)

        if(harSoktEllerMottarAap) {
            fjernKandidatForUtmeldingService.fjernKandidatForUtmelding(oppfolgingsperiode)
        } else {
            kandidatForUtmeldingService.handterUtmeldingsHendelse(
                fnr,
                AvsluttetAapSak(
                    oppfolgingsperiodeUuid = oppfolgingsperiode,
                    hendelseTidspunkt = Instant.now(),
                    avslutningsarsak = "Avsluttet AAP-sak",
                )
            )
        }
    }

}