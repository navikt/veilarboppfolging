package no.nav.veilarboppfolging.service

import no.nav.common.client.aktoroppslag.AktorOppslagClient
import no.nav.veilarboppfolging.client.aap.AapClient
import no.nav.veilarboppfolging.kafka.AapHendelseRecord
import no.nav.veilarboppfolging.kafka.Hendelse
import no.nav.veilarboppfolging.kandidatForUtmelding.FjernKandidatForUtmeldingService
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingRepository
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmeldingService
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.test.TestData.TEST_AKTOR_ID
import no.nav.veilarboppfolging.test.TestData.TEST_FNR
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.Optional

class AapHendelseServiceTest {
    private val aapClient: AapClient = mock()
    private val kandidatForUtmeldingService: KandidatForUtmeldingService = mock()
    private val aktorOppslagClient: AktorOppslagClient = mock()
    private val avsluttOppfolgingService: AvsluttOppfolgingService = mock()
    private val fjernKandidatForUtmeldingService: FjernKandidatForUtmeldingService = mock()
    private val kandidatForUtmeldingRepository: KandidatForUtmeldingRepository = mock()
    private val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository = mock()

    private val service = AapHendelseService(
        aapClient,
        kandidatForUtmeldingService,
        aktorOppslagClient,
        avsluttOppfolgingService,
        fjernKandidatForUtmeldingService,
        kandidatForUtmeldingRepository,
        oppfolgingsPeriodeRepository,
    )

    @Test
    fun `ignorerer hendelse naar bruker ikke har gjeldende oppfolgingsperiode`() {
        whenever(aktorOppslagClient.hentAktorId(TEST_FNR)).thenReturn(TEST_AKTOR_ID)
        whenever(oppfolgingsPeriodeRepository.hentGjeldendeOppfolgingsperiode(TEST_AKTOR_ID))
            .thenReturn(Optional.empty())

        service.håndterAapHendelse(TEST_FNR.get(), AapHendelseRecord(TEST_FNR.get(), Hendelse.VEDTAK))

        verify(aktorOppslagClient).hentAktorId(TEST_FNR)
        verify(oppfolgingsPeriodeRepository).hentGjeldendeOppfolgingsperiode(TEST_AKTOR_ID)
        verifyNoInteractions(
            aapClient,
            kandidatForUtmeldingService,
            avsluttOppfolgingService,
            fjernKandidatForUtmeldingService,
            kandidatForUtmeldingRepository,
        )
    }
}
