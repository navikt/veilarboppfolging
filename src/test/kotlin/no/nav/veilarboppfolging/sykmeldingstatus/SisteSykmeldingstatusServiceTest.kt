package no.nav.veilarboppfolging.sykmeldingstatus

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.client.isoppfolgingstilfelle.OppfolgingstilfelleDTO
import no.nav.veilarboppfolging.kafka.KafkaOppfolgingstilfelle
import no.nav.veilarboppfolging.kafka.KafkaOppfolgingstilfellePerson
import no.nav.veilarboppfolging.oppfolgingsbruker.BrukerRegistrant
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering.Companion.arbeidssokerRegistrering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SisteSykmeldingstatusServiceTest : IntegrationTest() {
    private val aktorId = AktorId.of("1234567890")
    private val fnr = Fnr.of("12345678901")

    @Test
    fun `hentOgLagreSisteSykmeldingstatus - bruker er sykmeldt - lagrer sykmeldingstatus`() {
        val oppfolgingsperiodeId = lagreOppfolgingsperiode(aktorId, fnr)
        val oppfolgingstilfelleDTO = OppfolgingstilfelleDTO(
            arbeidstakerAtTilfelleEnd = false,
            start = LocalDate.now().minusDays(2),
            end = LocalDate.now().plusDays(10),
            antallSykedager = null,
            varighetUker = 2,
            virksomhetsnummerList = emptyList(),
        )
        mockAktivtOppfolgingstilfelle(fnr, oppfolgingstilfelleDTO)

        sisteSykmeldingstatusService.hentOgLagreSisteSykmeldingstatus(
            oppfolgingsperiodeId = oppfolgingsperiodeId,
            fnr = fnr,
        )

        val sisteSykmeldingstatusFraDb = sisteSykmeldingstatusRepository.hentSisteSykmeldingstatus(oppfolgingsperiodeId)
        assertThat(sisteSykmeldingstatusFraDb).isNotNull
        assertThat(sisteSykmeldingstatusFraDb?.fnr).isEqualTo(fnr.get())
        assertThat(sisteSykmeldingstatusFraDb?.startdato).isEqualTo(oppfolgingstilfelleDTO.start)
        assertThat(sisteSykmeldingstatusFraDb?.sluttdato).isEqualTo(oppfolgingstilfelleDTO.end)
        assertThat(sisteSykmeldingstatusFraDb?.harArbeidsgiver).isEqualTo(oppfolgingstilfelleDTO.arbeidstakerAtTilfelleEnd)
    }

    @Test
    fun `hentOgLagreSisteSykmeldingstatus - bruker er ikke sykmeldt - lagrer ikke sykmeldingstatus`() {
        val oppfolgingsperiodeId = lagreOppfolgingsperiode(aktorId, fnr)
        mockAktivtOppfolgingstilfelle(fnr, null)

        sisteSykmeldingstatusService.hentOgLagreSisteSykmeldingstatus(
            oppfolgingsperiodeId = oppfolgingsperiodeId,
            fnr = fnr,
        )

        val sisteSykmeldingstatusFraDb = sisteSykmeldingstatusRepository.hentSisteSykmeldingstatus(oppfolgingsperiodeId)
        assertThat(sisteSykmeldingstatusFraDb).isNull()
    }

    @Test
    fun `handterMottattIsOppfolgingstilfelle - under oppfølging, aktivt syketilfelle - lagrer sykmeldingstatus`() {
        mockSytemBrukerAuthOk(aktorId, fnr)
        val oppfolgingsperiodeId = lagreOppfolgingsperiode(aktorId, fnr)
        val startdato = LocalDate.now().minusDays(2)
        val sluttdato = LocalDate.now().plusDays(10)
        val kafkaOppfolgingstilfellePerson = getKafkaOppfolgingstilfellePerson(
            fnr = fnr,
            start = startdato,
            end = sluttdato,
        )

        sisteSykmeldingstatusService.handterMottattIsOppfolgingstilfelle(
            key = UUID.randomUUID().toString(),
            kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePerson,
        )

        val sisteSykmeldingstatusFraDb = sisteSykmeldingstatusRepository.hentSisteSykmeldingstatus(oppfolgingsperiodeId)
        assertThat(sisteSykmeldingstatusFraDb).isNotNull
        assertThat(sisteSykmeldingstatusFraDb?.fnr).isEqualTo(fnr.get())
        assertThat(sisteSykmeldingstatusFraDb?.startdato).isEqualTo(startdato)
        assertThat(sisteSykmeldingstatusFraDb?.sluttdato).isEqualTo(sluttdato)
    }

    @Test
    fun `handterMottattIsOppfolgingstilfelle - under oppfølging, avsluttet syketilfelle - lagrer ikke sykmeldingstatus`() {
        mockSytemBrukerAuthOk(aktorId, fnr)
        val oppfolgingsperiodeId = lagreOppfolgingsperiode(aktorId, fnr)
        val startdato = LocalDate.now().minusMonths(2)
        val sluttdato = LocalDate.now().minusDays(10)
        val kafkaOppfolgingstilfellePerson = getKafkaOppfolgingstilfellePerson(
            fnr = fnr,
            start = startdato,
            end = sluttdato,
        )

        sisteSykmeldingstatusService.handterMottattIsOppfolgingstilfelle(
            key = UUID.randomUUID().toString(),
            kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePerson,
        )

        val sisteSykmeldingstatusFraDb = sisteSykmeldingstatusRepository.hentSisteSykmeldingstatus(oppfolgingsperiodeId)
        assertThat(sisteSykmeldingstatusFraDb).isNull()
    }

    @Test
    fun `handterMottattIsOppfolgingstilfelle - under oppfølging, lagret sykmeldingstatus, avsluttet syketilfelle - oppdaterer sykmeldingstatus`() {
        mockSytemBrukerAuthOk(aktorId, fnr)
        val oppfolgingsperiodeId = lagreOppfolgingsperiode(aktorId, fnr)
        val startdato = LocalDate.now().minusMonths(2)
        val sluttdato = LocalDate.now().minusDays(10)
        val kafkaOppfolgingstilfellePerson = getKafkaOppfolgingstilfellePerson(
            fnr = fnr,
            start = startdato,
            end = sluttdato,
        )
        sisteSykmeldingstatusRepository.lagreSisteSykmeldingstatus(
            SisteSykmeldingstatus(
                oppfolgingsperiodeId = oppfolgingsperiodeId,
                fnr = fnr.get(),
                startdato = LocalDate.now().minusMonths(3),
                sluttdato = LocalDate.now().plusDays(2),
                harArbeidsgiver = false,
            )
        )

        sisteSykmeldingstatusService.handterMottattIsOppfolgingstilfelle(
            key = UUID.randomUUID().toString(),
            kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePerson,
        )

        val sisteSykmeldingstatusFraDb = sisteSykmeldingstatusRepository.hentSisteSykmeldingstatus(oppfolgingsperiodeId)
        assertThat(sisteSykmeldingstatusFraDb).isNotNull
        assertThat(sisteSykmeldingstatusFraDb?.fnr).isEqualTo(fnr.get())
        assertThat(sisteSykmeldingstatusFraDb?.startdato).isEqualTo(startdato)
        assertThat(sisteSykmeldingstatusFraDb?.sluttdato).isEqualTo(sluttdato)
    }

    @Test
    fun `handterMottattIsOppfolgingstilfelle - avsluttet oppfølgingsperiode, aktivt syketilfelle - lagrer ikke sykmeldingstatus`() {
        mockSytemBrukerAuthOk(aktorId, fnr)
        mockTiltakshistorikk(fnr, harAktiveDeltakelser = false)
        mockUngdomsprogram(fnr, erDeltaker = false)
        mockAap(fnr, harAap = false)
        val oppfolgingsperiodeId = lagreOppfolgingsperiode(aktorId, fnr)
        setLocalArenaOppfolging(aktorId, Formidlingsgruppe.ARBS)
        avsluttOppfolgingManueltSomVeileder(aktorId)

        val startdato = LocalDate.now().minusDays(2)
        val sluttdato = LocalDate.now().plusDays(10)
        val kafkaOppfolgingstilfellePerson = getKafkaOppfolgingstilfellePerson(
            fnr = fnr,
            start = startdato,
            end = sluttdato,
        )

        sisteSykmeldingstatusService.handterMottattIsOppfolgingstilfelle(
            key = UUID.randomUUID().toString(),
            kafkaOppfolgingstilfellePerson = kafkaOppfolgingstilfellePerson,
        )

        val sisteSykmeldingstatusFraDb = sisteSykmeldingstatusRepository.hentSisteSykmeldingstatus(oppfolgingsperiodeId)
        assertThat(sisteSykmeldingstatusFraDb).isNull()
    }

    private fun lagreOppfolgingsperiode(aktorId: AktorId, fnr: Fnr): UUID {
        val oppfolgingsbruker = arbeidssokerRegistrering(fnr, aktorId, BrukerRegistrant(fnr))
        oppfolgingsStatusRepository.opprettOppfolging(aktorId)
        oppfolgingsPeriodeRepository.start(oppfolgingsbruker)
        return oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId).first().uuid
    }

    private fun getKafkaOppfolgingstilfellePerson(
        fnr: Fnr,
        start: LocalDate,
        end: LocalDate,
    ): KafkaOppfolgingstilfellePerson {
        return KafkaOppfolgingstilfellePerson(
            uuid = UUID.randomUUID().toString(),
            createdAt = OffsetDateTime.now(),
            personIdentNumber = fnr.get(),
            oppfolgingstilfelleList = listOf(
                KafkaOppfolgingstilfelle(
                    gradertAtTilfelleEnd = false,
                    arbeidstakerAtTilfelleEnd = false,
                    start = start,
                    end = end,
                    antallSykedager = null,
                    virksomhetsnummerList = emptyList(),
                )
            ),
            referanseTilfelleBitUuid = UUID.randomUUID().toString(),
            referanseTilfelleBitInntruffet = OffsetDateTime.now(),
            dodsdato = null,
            hasGjentakendeSykefravar = null,
        )
    }
}