package no.nav.veilarboppfolging.kandidatForUtmelding

import java.util.UUID
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Operasjon
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.service.AvsluttOppfolgingService
import no.nav.veilarboppfolging.service.KafkaProducerService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class KandidatForUtmeldingService(
    private val avsluttOppfolgingService: AvsluttOppfolgingService,
    private val kandidatForUtmeldingRepository: KandidatForUtmeldingRepository,
    private val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository,
    private val aktorOppslagClient: AktorOppslagClient,
    private val transactor: TransactionTemplate,
    private val kafkaProducerService: KafkaProducerService,
    @Value("\${app.sendUtmeldingskandidaterTilObo}") private val sendUtmeldingskandidaterTilObo: Boolean,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun lagreKandidatForUtmelding(fnr: Fnr, kandidatForUtmeldingHendelse: KandidatForUtmeldingHendelse) {
        // Vi sjekker avslutningsstatus for manuell avregistrering siden de bare blir kandidater for utmelding
        // Vi tar dem ikke ut av oppfølging automatisk
        transactor.executeWithoutResult { _ ->
            val avslutningsstatus = avsluttOppfolgingService.hentAvslutningstatusForManuellAvslutning(fnr)

            if (avslutningsstatus.kanAvslutte) {
                kandidatForUtmeldingRepository.lagreKandidat(kandidatForUtmeldingHendelse)
                logger.info("Kandidat ble lagret fordi arbeidssøkerperiode ble avsluttet, oppfølgingsperiode ${kandidatForUtmeldingHendelse.oppfolgingsperiodeUuid}")
                sendUtmeldingskandidatTilObo(kandidatForUtmeldingHendelse, fnr)
            } else {
                logger.info("Kandidat kunne ikke avsluttes selvom arbeidssøkerperiode ble avsluttet, oppfølgingsperiode ${kandidatForUtmeldingHendelse.oppfolgingsperiodeUuid}")
            }
        }
    }

    fun hentKandidatForUtmeldingTag(oppfolgingsperiodeId: UUID): KandidatForUtmeldingTag? {
        return kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeId)?.mapTilTag()
    }

    fun hentKandidatForUtmeldingTag(aktorId: AktorId): KandidatForUtmeldingTag? {
        val oppfolgingsperiodeId = oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId)?.getOrNull()?.uuid ?: return null
        return hentKandidatForUtmeldingTag(oppfolgingsperiodeId)
    }

    fun behandleKandidaterMedUtloptForlengelse() {
        val kandidaterMedUtloptForlengelse = kandidatForUtmeldingRepository.hentKandidaterMedUtloptForlengelse()
        logger.info("Behandler ${kandidaterMedUtloptForlengelse.size} kandidater med utløpt forlengelse")

        kandidaterMedUtloptForlengelse.forEach { kandidat ->
            transactor.executeWithoutResult { _ ->
                val oppfolgingsperiodeId = kandidat.oppfolgingsperiodeUuid
                val fnr = finnFnrForOppfolgingsperiode(oppfolgingsperiodeId)
                val avslutningsstatus = avsluttOppfolgingService.hentAvslutningstatusForManuellAvslutning(fnr)

                if (avslutningsstatus.kanAvslutte) {
                    logger.info("Kandidat for utmelding med oppfølgingsperiode $oppfolgingsperiodeId har utløpt forlengelse og kan avsluttes")
                    sendUtmeldingskandidatTilObo(kandidat, fnr)
                    kandidatForUtmeldingRepository.nullstillForlengetTil(oppfolgingsperiodeId)
                } else {
                    logger.info("Kandidat for utmelding med oppfølgingsperiode $oppfolgingsperiodeId har utløpt forlengelse, men kan ikke avsluttes")
                    kandidatForUtmeldingRepository.fjernKandidat(oppfolgingsperiodeId)
                }
            }
        }
        logger.info("Ferdig med å behandle kandidater med utløpt forlengelse")
    }

    private fun sendUtmeldingskandidatTilObo(kandidat: KandidatForUtmeldingHendelse, fnr: Fnr) {
        if (sendUtmeldingskandidaterTilObo) {
            val filterkategoriPersonId =
                kandidatForUtmeldingRepository.hentEllerOpprettFilterhendelseId(kandidat.oppfolgingsperiodeUuid)
            logger.info("Sender kandidat for utmelding til OBO med key=$filterkategoriPersonId for oppfølgingsperiode ${kandidat.oppfolgingsperiodeUuid}")
            val filterhendelse = kandidat.tilFilterhendelseRecord(fnr, Operasjon.START)
            kafkaProducerService.publiserFilterhendelse(filterkategoriPersonId, filterhendelse)
        } else {
            logger.info("Sender ikke kandidat for utmelding til OBO for oppfølgingsperiode ${kandidat.oppfolgingsperiodeUuid} fordi sending til OBO er togglet av")
        }
    }

    private fun finnFnrForOppfolgingsperiode(oppfolgingsperiodeId: UUID): Fnr {
        val aktorId = oppfolgingsPeriodeRepository.hentOppfolgingsperiode(oppfolgingsperiodeId.toString())
            .getOrElse { throw IllegalStateException("Oppfølgingsperiode med id $oppfolgingsperiodeId finnes ikke") }?.aktorId
        return aktorOppslagClient.hentFnr(AktorId(aktorId))
    }

    fun forlengKandidat(hendelse: ForlengelseHendelse) {
        logger.info("Lagrer forlengelse for oppfølgingsperiode ${hendelse.oppfolgingsperiodeUuid}")
        kandidatForUtmeldingRepository.lagreKandidat(hendelse)
    }
}