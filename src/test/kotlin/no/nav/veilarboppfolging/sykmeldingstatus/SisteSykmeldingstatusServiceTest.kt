package no.nav.veilarboppfolging.sykmeldingstatus

import java.time.LocalDate
import java.util.UUID
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.client.isoppfolgingstilfelle.OppfolgingstilfelleDTO
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
            oppfolgingsperiodeId = oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId).get().uuid,
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
            oppfolgingsperiodeId = oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(aktorId).get().uuid,
            fnr = fnr,
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
}