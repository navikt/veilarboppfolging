package no.nav.veilarboppfolging.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.client.aap.AapClient
import no.nav.veilarboppfolging.kafka.AapHendelseRecord
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AapHendelseService(
    val aapClient: AapClient,
    val kandidatForUtmeldingService: KandidatForUtmeldingService,
    val aktorOppslagClient: AktorOppslagClient,
    val avsluttOppfolgingService: AvsluttOppfolgingService
) {
    val log = LoggerFactory.getLogger(this::class.java)

    fun håndterAapHendelse(key: String, value: AapHendelseRecord) {
        // ingen aktiv periode -> avslag / evt kelvin status avsluttet/ferdigbehandlet?
        //
        log.info("Mottatt AAP hendelse: ${value.hendelse}")
        val aapSaker = aapClient.hentSaker(value.ident)
        val mottarIkkeAap = aapSaker.harIkkeAap()
        val harAap = aapSaker.mottarEllerHarSoktAAP()
        val erKandidatForUtmelding = kandidatForUtmeldingService.erAktivUtmeldingskandidat(AktorId.of(key))

        val kanAvsluttes by lazy { avsluttOppfolgingService.hentAvslutningstatusForManuellAvslutning(Fnr.of(key)).kanAvslutte }

        log.info("AAP saker: ${aapSaker.saker}. Mottar ikke AAP: $mottarIkkeAap. HarAap: $harAap")
        when {
            mottarIkkeAap && !erKandidatForUtmelding && kanAvsluttes -> {
                // skal legges til som kandidat og fjernes fra "kandidater som ikke kan avsluttes
            }
             harAap && erKandidatForUtmelding -> {
                // skal fjernes som kandidat
            }
            mottarIkkeAap && erKandidatForUtmelding -> {
                // ignorer
            }
            mottarIkkeAap && !erKandidatForUtmelding && !kanAvsluttes -> {
                // ignorer
            }
            !erKandidatForUtmelding && harAap -> {
                // ignorer
            }
            !kanAvsluttes && harAap -> {
                // fjern fra "kandidater som ikke kan avsluttes"
            }
        }
    }

}