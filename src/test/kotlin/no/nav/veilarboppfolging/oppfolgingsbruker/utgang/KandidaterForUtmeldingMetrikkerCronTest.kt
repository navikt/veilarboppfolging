package no.nav.veilarboppfolging.oppfolgingsbruker.utgang

import no.nav.common.job.leader_election.LeaderElectionClient
import no.nav.veilarboppfolging.eventsLogger.BigQueryClient
import no.nav.veilarboppfolging.eventsLogger.KandidaterForUtmeldingMetrikker
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingRepository
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify

class KandidaterForUtmeldingMetrikkerCronTest {
    private val bigQueryClient: BigQueryClient = Mockito.mock(BigQueryClient::class.java)
    private val kandidatForUtmeldingRepository: KandidatForUtmeldingRepository = Mockito.mock(KandidatForUtmeldingRepository::class.java)
    private val oppfolgingsStatusRepository: OppfolgingsStatusRepository = Mockito.mock(OppfolgingsStatusRepository::class.java)
    private val leaderElectionClient: LeaderElectionClient = Mockito.mock(LeaderElectionClient::class.java)

    private val cron = KandidaterForUtmeldingMetrikkerCron(
        bigQueryClient = bigQueryClient,
        kandidatForUtmeldingRepository = kandidatForUtmeldingRepository,
        oppfolgingsStatusRepository = oppfolgingsStatusRepository,
        leaderElectionClient = leaderElectionClient,
    )

    @Test
    fun `loggKandidaterForUtmeldingMetrikker sender alle metrikker til bigquery`() {
        Mockito.`when`(kandidatForUtmeldingRepository.hentAntallKandidaterForUtmelding()).thenReturn(10)
        Mockito.`when`(oppfolgingsStatusRepository.hentAntallUnderOppfolgingMedIserv()).thenReturn(4)
        Mockito.`when`(kandidatForUtmeldingRepository.hentAntallKandidaterForUtmeldingIkkeForlenget()).thenReturn(6)
        Mockito.`when`(kandidatForUtmeldingRepository.hentAntallKandidaterForUtmeldingForlenget()).thenReturn(4)

        cron.loggKandidaterForUtmeldingMetrikker()

        val captor = argumentCaptor<KandidaterForUtmeldingMetrikker>()
        verify(bigQueryClient).loggKandidaterForUtmeldingMetrikker(captor.capture())
        assertThat(captor.firstValue).isEqualTo(
            KandidaterForUtmeldingMetrikker(
                antallKandidaterForUtmelding = 10,
                antallUnderOppfolgingMedIserv = 4,
                antallKandidaterForUtmeldingIkkeForlenget = 6,
                antallKandidaterForUtmeldingForlenget = 4,
            )
        )
    }
}
