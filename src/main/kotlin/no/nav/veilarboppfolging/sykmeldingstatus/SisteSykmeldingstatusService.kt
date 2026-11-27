package no.nav.veilarboppfolging.sykmeldingstatus

import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.jvm.optionals.getOrNull
import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.client.isoppfolgingstilfelle.IsOppfolgingstilfelleClient
import no.nav.veilarboppfolging.kafka.KafkaOppfolgingstilfellePerson
import no.nav.veilarboppfolging.kandidatForUtmelding.FjernKandidatForUtmeldingService
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmelding
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingService
import no.nav.veilarboppfolging.kandidatForUtmelding.hendelser.ForlengelseHendelseType
import no.nav.veilarboppfolging.kandidatForUtmelding.hendelser.IkkeLengerSykmeldtHendelseType
import no.nav.veilarboppfolging.kandidatForUtmelding.hendelser.IkkeLengerSykmeldtHendelse
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

@Service
class SisteSykmeldingstatusService(
    private val sisteSykmeldingstatusRepository: SisteSykmeldingstatusRepository,
    private val isOppfolgingstilfelleClient: IsOppfolgingstilfelleClient,
    private val aktorOppslagClient: AktorOppslagClient,
    private val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository,
    private val transactor: TransactionTemplate,
    private val kandidatForUtmeldingService: KandidatForUtmeldingService,
    private val fjernKandidatForUtmeldingService: FjernKandidatForUtmeldingService,
) {
    val log = LoggerFactory.getLogger(this::class.java)

    fun hentOgLagreSisteSykmeldingstatus(oppfolgingsperiodeId: UUID, fnr: Fnr) {
        log.info("Henter siste oppfolgingstilfelle fra isyfo for oppfølgingsperiode $oppfolgingsperiodeId")
        val sisteOppfolgingstilfelle = isOppfolgingstilfelleClient.hentAktivtOppfolgingstilfelle(fnr)

        if (sisteOppfolgingstilfelle != null) {
            val sisteSykmeldingstatus = SisteSykmeldingstatus(
                oppfolgingsperiodeId = oppfolgingsperiodeId,
                fnr = fnr.get(),
                startdato = sisteOppfolgingstilfelle.start,
                sluttdato = sisteOppfolgingstilfelle.end,
                harArbeidsgiver = sisteOppfolgingstilfelle.arbeidstakerAtTilfelleEnd,
            )
            sisteSykmeldingstatusRepository.lagreSisteSykmeldingstatus(sisteSykmeldingstatus)
            log.info("Oppdatert siste sykmeldingstatus for oppfølgingsperiode $oppfolgingsperiodeId")
        }
    }

    @Transactional
    fun handterMottattIsOppfolgingstilfelle(key: String, kafkaOppfolgingstilfellePerson: KafkaOppfolgingstilfellePerson) {
        log.info("Mottatt syketilfelle for key $key")
        val fnr = Fnr.of(kafkaOppfolgingstilfellePerson.personIdentNumber)
        val aktorId = aktorOppslagClient.hentAktorId(fnr)

        val oppfolgingsperiodeId = oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId)
            .getOrNull()?.uuid
        if (oppfolgingsperiodeId == null) {
            log.info("Ignorerer syketilfelle med key $key: bruker har ingen gjeldende oppfolgingsperiode")
            return
        }

        val aktivtOppfolgingstilfelle = kafkaOppfolgingstilfellePerson.finnAktivtOppfolgingstilfelle()
        if (aktivtOppfolgingstilfelle != null) {
            val sisteSykmeldingstatus = SisteSykmeldingstatus(
                oppfolgingsperiodeId = oppfolgingsperiodeId,
                fnr = kafkaOppfolgingstilfellePerson.personIdentNumber,
                startdato = aktivtOppfolgingstilfelle.start,
                sluttdato = aktivtOppfolgingstilfelle.end,
                harArbeidsgiver = aktivtOppfolgingstilfelle.arbeidstakerAtTilfelleEnd,
            )
            sisteSykmeldingstatusRepository.lagreSisteSykmeldingstatus(sisteSykmeldingstatus)
            log.info("Lagret eller oppdaterte siste sykmeldingstatus for oppfølgingsperiode $oppfolgingsperiodeId, key $key")

            if (!aktivtOppfolgingstilfelle.arbeidstakerAtTilfelleEnd) {
                log.info("Bruker med oppfølgingsperiode $oppfolgingsperiodeId er sykmeldt uten arbeidsgiver, fjerner evt kandidat for utmelding")
                fjernKandidatForUtmeldingService.fjernKandidatForUtmelding(oppfolgingsperiodeId)
            } else {
                val kandidat = kandidatForUtmeldingService.hentAktivEllerForlengetKandidat(oppfolgingsperiodeId) ?: return

                if (kandidat.erIkkeLengerSykmeldtMedArbeidsgiver()) {
                    log.info("Bruker med oppfølgingsperiode $oppfolgingsperiodeId er kandidat pga sykmeldt med arbeidsgiver, fjerner kandidat for utmelding")
                    fjernKandidatForUtmeldingService.fjernKandidatForUtmelding(oppfolgingsperiodeId)
                } else if (kandidat.erForlenget() && kandidat.sisteHendelseForForlengelseVarSykmeldtMedArbeidsgiver()) {
                    log.info("Bruker med oppfølgingsperiode $oppfolgingsperiodeId er forlenget kandidat pga sykmeldt med arbeidsgiver, fjerner kandidat for utmelding")
                    fjernKandidatForUtmeldingService.fjernKandidatForUtmelding(oppfolgingsperiodeId)
                }
            }
            return
        }

        val oppfolgingstilfelleMedSisteSluttdato =
            kafkaOppfolgingstilfellePerson.finnOppfolgingstilfelleMedSisteSluttdato()
        if (oppfolgingstilfelleMedSisteSluttdato != null && sisteSykmeldingstatusRepository.hentSisteSykmeldingstatus(oppfolgingsperiodeId) != null) {
            val sisteSykmeldingstatus = SisteSykmeldingstatus(
                oppfolgingsperiodeId = oppfolgingsperiodeId,
                fnr = kafkaOppfolgingstilfellePerson.personIdentNumber,
                startdato = oppfolgingstilfelleMedSisteSluttdato.start,
                sluttdato = oppfolgingstilfelleMedSisteSluttdato.end,
                harArbeidsgiver = oppfolgingstilfelleMedSisteSluttdato.arbeidstakerAtTilfelleEnd,
            )
            sisteSykmeldingstatusRepository.lagreSisteSykmeldingstatus(sisteSykmeldingstatus)
            log.info("Oppdaterte siste sykmeldingstatus for oppfølgingsperiode $oppfolgingsperiodeId for bruker som ikke lenger er sykmeldt, key $key")
        } else {
            log.info("Ignorerer syketilfelle som ikke lenger er aktivt siden vi ikke har lagret sykmeldingstatus for oppfølgingsperiode $oppfolgingsperiodeId fra før, key $key")
        }
    }

    fun opprettKandidaterForUtmelding() {
        val avsluttetTidligereEnn = LocalDate.now().minusDays(16)
        val sisteSykmeldingstatusIkkeLengerSykmeldt = sisteSykmeldingstatusRepository.hentSykmeldteMedStatusAvsluttetSomIkkeErBehandlet(avsluttetTidligereEnn)
        log.info("Behandler ${sisteSykmeldingstatusIkkeLengerSykmeldt.size} oppfølgingsperioder der syketilfellet ble avsluttet for mer enn 16 dager siden")

        sisteSykmeldingstatusIkkeLengerSykmeldt.forEach { sisteSykmeldingstatus ->
            try {
                transactor.executeWithoutResult { _ ->
                    kandidatForUtmeldingService.handterUtmeldingsHendelse(
                        IkkeLengerSykmeldtHendelse(
                            oppfolgingsperiodeUuid = sisteSykmeldingstatus.oppfolgingsperiodeId,
                            syketilfelleAvsluttetDato = sisteSykmeldingstatus.sluttdato,
                            hendelseTidspunkt = ZonedDateTime.now().toInstant(),
                            ikkeLengerSykmeldtHendelseType = if (sisteSykmeldingstatus.harArbeidsgiver) {
                                IkkeLengerSykmeldtHendelseType.IKKE_LENGER_SYKMELDT_MED_ARBEIDSGIVER
                            } else {
                                IkkeLengerSykmeldtHendelseType.IKKE_LENGER_SYKMELDT_UTEN_ARBEIDSGIVER
                            }
                        )
                    )
                    sisteSykmeldingstatusRepository.oppdaterIkkeLengerSykmeldtUtenArbeidsgiverHendelseUtlost(sisteSykmeldingstatus.oppfolgingsperiodeId)
                }
            } catch (e: Exception) {
                log.error("Feil ved behandling av oppfølgingsperiode med id ${sisteSykmeldingstatus.oppfolgingsperiodeId} med avsluttet syketilfelle", e)
            }
        }
        log.info("Ferdig med å behandle oppfølgingsperioder med avsluttet syketilfelle")
    }

    private fun KandidatForUtmelding.erIkkeLengerSykmeldtMedArbeidsgiver(): Boolean {
        return this.sisteHendelse.type == IkkeLengerSykmeldtHendelseType.IKKE_LENGER_SYKMELDT_MED_ARBEIDSGIVER
    }

    private fun KandidatForUtmelding.erForlenget(): Boolean {
        return this.sisteHendelse.type is ForlengelseHendelseType
    }

    private fun KandidatForUtmelding.sisteHendelseForForlengelseVarSykmeldtMedArbeidsgiver(): Boolean {
        val kandidathendelser = kandidatForUtmeldingService.hentUtmeldingsKandidatHendelser(oppfolgingsperiodeId)
        val sisteHendelseIkkeForlengelse = kandidathendelser.firstOrNull { it.type !is ForlengelseHendelseType }
        return sisteHendelseIkkeForlengelse?.type == IkkeLengerSykmeldtHendelseType.IKKE_LENGER_SYKMELDT_MED_ARBEIDSGIVER
    }
}