package no.nav.veilarboppfolging.kandidatForUtmelding

import java.util.UUID
import kotlin.jvm.optionals.getOrNull
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.FilterhendelseRecord
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Operasjon
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.service.AvsluttOppfolgingService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate

@Service
class KandidatForUtmeldingService(
    private val avsluttOppfolgingService: AvsluttOppfolgingService,
    private val kandidatForUtmeldingRepository: KandidatForUtmeldingRepository,
    private val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository,
    private val transactor: TransactionTemplate,
    private val kandidatForUtmeldingKafkaPubliseringService: KandidatForUtmeldingKafkaPubliseringService,
    @Value("\${app.sendUtmeldingskandidaterTilObo}") private val sendUtmeldingskandidaterTilObo: Boolean,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun lagreKandidatForUtmelding(fnr: Fnr, kandidatForUtmeldingHendelse: KandidatForUtmeldingHendelse) {
        // Vi sjekker avslutningsstatus for manuell avregistrering siden de bare blir kandidater for utmelding
        // Vi tar dem ikke ut av oppfølging automatisk
        val publiseringsdata = transactor.execute {
            val avslutningsstatus = avsluttOppfolgingService.hentAvslutningstatusForManuellAvslutning(fnr)

            if (avslutningsstatus.kanAvslutte) {
                val utmeldingshendelseId = kandidatForUtmeldingRepository.lagreKandidat(kandidatForUtmeldingHendelse)
                logger.info("Kandidat ble lagret fordi arbeidssøkerperiode ble avsluttet, oppfølgingsperiode ${kandidatForUtmeldingHendelse.oppfolgingsperiodeUuid}")

                if (sendUtmeldingskandidaterTilObo) {
                    val filterkategoriPersonId = kandidatForUtmeldingRepository.hentEllerOpprettFilterhendelseId(kandidatForUtmeldingHendelse.oppfolgingsperiodeUuid)
                    logger.info("Sender kandidat for utmelding til OBO med key=$filterkategoriPersonId for oppfølgingsperiode ${kandidatForUtmeldingHendelse.oppfolgingsperiodeUuid}")
                    val filterhendelse = kandidatForUtmeldingHendelse.tilFilterhendelseRecord(fnr, Operasjon.START)
                    Publiseringsdata(
                        utmeldingshendelseId = utmeldingshendelseId,
                        filterkategoriPersonId = filterkategoriPersonId,
                        filterhendelse = filterhendelse,
                    )
                } else {
                    logger.info("Sender ikke kandidat for utmelding til OBO for oppfølgingsperiode ${kandidatForUtmeldingHendelse.oppfolgingsperiodeUuid} fordi sending til OBO er togglet av")
                    null
                }
            } else {
                logger.info("Kandidat kunne ikke avsluttes selvom arbeidssøkerperiode ble avsluttet, oppfølgingsperiode ${kandidatForUtmeldingHendelse.oppfolgingsperiodeUuid}")
                null
            }
        }

        publiseringsdata?.let { publiseringsdata ->
            kandidatForUtmeldingKafkaPubliseringService.publiserOgLoggKafkaMelding(
                utmeldingshendelseId = publiseringsdata.utmeldingshendelseId,
                filterkategoriPersonId = publiseringsdata.filterkategoriPersonId,
                filterhendelse = publiseringsdata.filterhendelse
            )
        }
    }

    fun hentKandidatForUtmeldingTag(oppfolgingsperiodeId: UUID): KandidatForUtmeldingTag? {
        return kandidatForUtmeldingRepository.hentKandidat(oppfolgingsperiodeId)?.mapTilTag()
    }

    fun hentKandidatForUtmeldingTag(aktorId: AktorId): KandidatForUtmeldingTag? {
        val oppfolgingsperiodeId = oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId)?.getOrNull()?.uuid ?: return null
        return hentKandidatForUtmeldingTag(oppfolgingsperiodeId)
    }
}

private data class Publiseringsdata(
    val utmeldingshendelseId: UUID,
    val filterkategoriPersonId: UUID,
    val filterhendelse: FilterhendelseRecord,
)