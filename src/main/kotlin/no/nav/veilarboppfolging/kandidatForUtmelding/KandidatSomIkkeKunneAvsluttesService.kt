package no.nav.veilarboppfolging.kandidatForUtmelding

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.jvm.optionals.getOrElse
import no.nav.common.types.identer.AktorId
import no.nav.veilarboppfolging.kandidatForUtmelding.hendelser.InaktivertIArena
import no.nav.veilarboppfolging.service.OppfolgingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class KandidatSomIkkeKunneAvsluttesService(
    private val oppfolgingService: OppfolgingService,
    private val kandidatForUtmeldingRepository: KandidatForUtmeldingRepository,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun lagreInaktivertIArenaSomIkkeKunneAvsluttes(
        aktorId: AktorId,
        iservFraDato: LocalDate?,
        begrunnelse: String?,
    ) {
        val oppfolgingsperiodeId = oppfolgingService.hentGjeldendeOppfolgingsperiode(aktorId).getOrElse {
            logger.warn("Fant ikke oppfølgingsperiode for bruker som ble inaktivert i Arena")
            return
        }.uuid
        val erForlengetEllerAktivKandidatForUtmelding = kandidatForUtmeldingRepository.erAktivEllerForlengetKandidatForUtmelding(oppfolgingsperiodeId)

        if (erForlengetEllerAktivKandidatForUtmelding) {
            logger.info("Oppfølgingsperiode med id $oppfolgingsperiodeId er allerede kandidat for utmelding, ignorerer inaktivering i Arena")
            return
        }
        val inaktivertIArenaHendelse = InaktivertIArena(
            oppfolgingsperiodeUuid = oppfolgingsperiodeId,
            iservFraDato = iservFraDato,
            hendelseTidspunkt = iservFraDato?.atStartOfDay()?.atZone(ZoneId.systemDefault())?.toInstant() ?: Instant.now(),
        )
        kandidatForUtmeldingRepository.lagreKandidatSomIkkeKunneAvsluttesOgHendelse(KandidatSomIkkeKanAvsluttes(inaktivertIArenaHendelse, begrunnelse))
    }
}