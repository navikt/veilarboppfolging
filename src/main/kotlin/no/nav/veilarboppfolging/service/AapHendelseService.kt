package no.nav.veilarboppfolging.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.veilarboppfolging.client.aap.AapClient
import no.nav.veilarboppfolging.kafka.AapHendelseRecord
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingService
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AapHendelseService(
    val aapClient: AapClient,
    val kandidatForUtmeldingService: KandidatForUtmeldingService,
    val aktorOppslagClient: AktorOppslagClient,
    val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository
) {
    val log = LoggerFactory.getLogger(this::class.java)

    fun håndterAapHendelse(key: String, value: AapHendelseRecord) {
        log.info("Mottatt AAP hendelse: ${value.hendelse}")
//        val aktorId = aktorOppslagClient.hentAktorId(Fnr.of(value.ident))
//        val gjeldendeOppfolgingsperiode = oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId)
//
//        val harAap = aapClient.harAap(key)
//        val erKandidatForUtmelding = kandidatForUtmeldingService.erAktivUtmeldingskandidat(key)
    }

}