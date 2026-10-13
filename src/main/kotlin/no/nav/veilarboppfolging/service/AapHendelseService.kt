package no.nav.veilarboppfolging.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.client.aap.AapClient
import no.nav.veilarboppfolging.kafka.AapHendelseRecord
import no.nav.veilarboppfolging.kandidatForUtmelding.FjernKandidatForUtmeldingService
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingRepository
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingService
import no.nav.veilarboppfolging.kandidatForUtmelding.hendelser.IkkeLengerAap
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class AapHendelseService(
    val aapClient: AapClient,
    val kandidatForUtmeldingService: KandidatForUtmeldingService,
    val aktorOppslagClient: AktorOppslagClient,
    val avsluttOppfolgingService: AvsluttOppfolgingService,
    val fjernKandidatForUtmeldingService: FjernKandidatForUtmeldingService,
    val kandidatForUtmeldingRepository: KandidatForUtmeldingRepository,
    private val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository
) {
    val log = LoggerFactory.getLogger(this::class.java)

    fun håndterAapHendelse(key: String, value: AapHendelseRecord) {
        log.info("Mottatt AAP hendelse: ${value.hendelse}")
        val aapSaker = aapClient.hentSaker(value.ident)
        val erKandidatForUtmelding = kandidatForUtmeldingService.erAktivUtmeldingskandidat(AktorId.of(key))
        val oppfolgingsperiode = oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(AktorId.of(key))
            .orElseThrow { IllegalStateException("Mangler gjeldende oppfolgingsperiode ved behandling av AAP-hendelse") }
            .uuid
        val avslutningsstatus by lazy { avsluttOppfolgingService.hentAvslutningstatusForManuellAvslutning(Fnr.of(key)) }
        val kanAvsluttes = avslutningsstatus.kanAvslutte
        val harIkkeSoktEllerMottarAap = !avslutningsstatus.harAap

        log.info("AAP saker: ${aapSaker.saker}. Mottar ikke AAP: $harIkkeSoktEllerMottarAap.")
        when {
             !harIkkeSoktEllerMottarAap && erKandidatForUtmelding -> {
                fjernKandidatForUtmeldingService.fjernKandidatForUtmelding(oppfolgingsperiode)
            }
            harIkkeSoktEllerMottarAap && erKandidatForUtmelding -> {
                return
            }
            !erKandidatForUtmelding && !harIkkeSoktEllerMottarAap -> {
                return
            }
            !erKandidatForUtmelding && kanAvsluttes -> {
                kandidatForUtmeldingService.handterUtmeldingsHendelse(
                    Fnr.of(key),
                    IkkeLengerAap(
                        oppfolgingsperiodeUuid = oppfolgingsperiode,
                        hendelseTidspunkt = Instant.now(),
                        avslutningsarsak = "Ikke lenger AAP",
                    )
                )
                kandidatForUtmeldingRepository.fjernKandidatSomIkkeKunneAvsluttes(oppfolgingsperiode)
            }
            harIkkeSoktEllerMottarAap && !kanAvsluttes -> {
                return
            }
            !kanAvsluttes && !harIkkeSoktEllerMottarAap -> {
                kandidatForUtmeldingRepository.fjernKandidatSomIkkeKunneAvsluttes(oppfolgingsperiode)
            }
        }
    }

}