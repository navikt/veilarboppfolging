package no.nav.veilarboppfolging.service

import no.nav.common.types.identer.AktorId
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.ArenaOppfolgingService
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.EndringPaaOppfolgingsBruker
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.LocalArenaOppfolging
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingsRegistrering
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.ArenaIservKanIkkeReaktiveres
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.Avregistrering
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity
import no.nav.veilarboppfolging.test.TestData
import no.nav.veilarboppfolging.test.TestData.TEST_AKTOR_ID
import no.nav.veilarboppfolging.test.TestData.TEST_FNR
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.util.Optional

class OppfolgingEndringServiceTest {
    private val authService: AuthService = Mockito.mock<AuthService>(AuthService::class.java)
    private val oppfolgingService: OppfolgingService = Mockito.mock<OppfolgingService>(OppfolgingService::class.java)
    private val startOppfolgingService: StartOppfolgingService = Mockito.mock<StartOppfolgingService>(StartOppfolgingService::class.java)
    private val arenaOppfolgingService: ArenaOppfolgingService =
        Mockito.mock<ArenaOppfolgingService>(ArenaOppfolgingService::class.java)
    private val kvpService: KvpService = Mockito.mock<KvpService>(KvpService::class.java)
    private val metricsService: MetricsService? = Mockito.mock<MetricsService?>(MetricsService::class.java)
    private val oppfolgingsStatusRepository: OppfolgingsStatusRepository =
        Mockito.mock<OppfolgingsStatusRepository>(OppfolgingsStatusRepository::class.java)

    private val oppfolgingEndringService = OppfolgingEndringService(
        oppfolgingService, startOppfolgingService, arenaOppfolgingService,
        kvpService, metricsService, oppfolgingsStatusRepository
    )

    @Test
    fun oppdaterOppfolgingMedStatusFraArena__skal_ikke_oppdatere_hvis_bruker_ikke_under_oppfolging_i_veilarboppfolging_eller_arena() {
        Mockito.`when`<AktorId?>(authService.getAktorIdOrThrow(TEST_FNR)).thenReturn(TEST_AKTOR_ID)
        Mockito.`when`<Optional<OppfolgingEntity>?>(oppfolgingsStatusRepository.hentOppfolging(TEST_AKTOR_ID))
            .thenReturn(
                Optional.of<OppfolgingEntity>(OppfolgingEntity().setUnderOppfolging(false))
            )

        val brukverV2 = EndringPaaOppfolgingsBruker(
            aktorId =  TEST_AKTOR_ID,
            fodselsnummer = TEST_FNR.get(),
            formidlingsgruppe = Formidlingsgruppe.ISERV,
            kvalifiseringsgruppe = Kvalifiseringsgruppe.VURDI
        )

        oppfolgingEndringService.oppdaterOppfolgingMedStatusFraArena(brukverV2)

        Mockito.verify<StartOppfolgingService?>(startOppfolgingService, Mockito.never()).startOppfolgingHvisIkkeAlleredeStartet(
            ArgumentMatchers.any<OppfolgingsRegistrering?>(OppfolgingsRegistrering::class.java)
        )
        Mockito.verify<OppfolgingService?>(oppfolgingService, Mockito.never())
            .avsluttOppfolging(ArgumentMatchers.any<Avregistrering?>())
    }

    @Test
    fun oppdaterOppfolgingMedStatusFraArena__skal_starte_oppfolging_pa_bruker_som_ikke_er_under_oppfolging_i_veilarboppfolging_men_under_oppfolging_i_arena() {
        Mockito.`when`<AktorId?>(authService.getAktorIdOrThrow(TEST_FNR)).thenReturn(TEST_AKTOR_ID)
        Mockito.`when`<Optional<OppfolgingEntity>?>(oppfolgingsStatusRepository.hentOppfolging(TEST_AKTOR_ID))
            .thenReturn(
                Optional.of<OppfolgingEntity>(OppfolgingEntity().setUnderOppfolging(false))
            )

        val brukverV2 = EndringPaaOppfolgingsBruker(
            aktorId =  TEST_AKTOR_ID,
            fodselsnummer = TEST_FNR.get(),
            formidlingsgruppe = Formidlingsgruppe.ARBS,
            kvalifiseringsgruppe = Kvalifiseringsgruppe.VURDI
        )

        oppfolgingEndringService.oppdaterOppfolgingMedStatusFraArena(brukverV2)

        Mockito.verify(startOppfolgingService, Mockito.times(1)).startOppfolgingHvisIkkeAlleredeStartet(
            OppfolgingsRegistrering.Companion.arenaSyncOppfolgingBruker(
                TEST_AKTOR_ID,
                Formidlingsgruppe.ARBS,
                brukverV2.kvalifiseringsgruppe
            )
        )
        Mockito.verify(oppfolgingService, Mockito.never())
            .avsluttOppfolging(ArgumentMatchers.any<Avregistrering?>())
    }

    @Test
    fun oppdaterOppfolgingMedStatusFraArena__skal_avslutte_oppfolging_pa_bruker_som_er_under_oppfolging_i_veilarboppfolging_men_ikke_under_oppfolging_i_arena() {
        Mockito.`when`<AktorId?>(authService.getAktorIdOrThrow(TEST_FNR)).thenReturn(TEST_AKTOR_ID)
        Mockito.`when`<Optional<OppfolgingEntity>?>(oppfolgingsStatusRepository.hentOppfolging(TEST_AKTOR_ID))
            .thenReturn(
                Optional.of<OppfolgingEntity>(
                    OppfolgingEntity()
                        .setLocalArenaOppfolging(Optional.empty<LocalArenaOppfolging?>())
                        .setUnderOppfolging(true)
                )
            )
        Mockito.`when`<Optional<Boolean>?>(arenaOppfolgingService.kanEnkeltReaktiveres(TEST_FNR)).thenReturn(
            Optional.of<Boolean>(false)
        )
        Mockito.`when`<Boolean?>(kvpService.erUnderKvp(TEST_AKTOR_ID)).thenReturn(false)

        val brukverV2 = EndringPaaOppfolgingsBruker(
          aktorId = TEST_AKTOR_ID,
            fodselsnummer = TEST_FNR.get(),
            formidlingsgruppe = Formidlingsgruppe.ISERV,
            kvalifiseringsgruppe = Kvalifiseringsgruppe.VURDI
        )

        oppfolgingEndringService.oppdaterOppfolgingMedStatusFraArena(brukverV2)

        Mockito.verify(startOppfolgingService, Mockito.never()).startOppfolgingHvisIkkeAlleredeStartet(
            ArgumentMatchers.any(OppfolgingsRegistrering::class.java)
        )
        Mockito.verify(oppfolgingService, Mockito.times(1))
            .avsluttOppfolging(ArenaIservKanIkkeReaktiveres(TestData.TEST_AKTOR_ID))

        Mockito.verify<MetricsService?>(metricsService, Mockito.times(1))
            .rapporterAutomatiskAvslutningAvOppfolging(true)
    }

    @Test
    fun oppdaterOppfolgingMedStatusFraArena__skal_ikke_avslutte_oppfolging_pa_bruker_som_kan_enkelt_reaktiveres() {
        Mockito.`when`<AktorId?>(authService.getAktorIdOrThrow(TEST_FNR)).thenReturn(TEST_AKTOR_ID)
        Mockito.`when`<Optional<OppfolgingEntity>?>(oppfolgingsStatusRepository.hentOppfolging(TEST_AKTOR_ID))
            .thenReturn(
                Optional.of<OppfolgingEntity>(
                    OppfolgingEntity()
                        .setLocalArenaOppfolging(Optional.empty<LocalArenaOppfolging?>())
                        .setUnderOppfolging(true)
                )
            )
        Mockito.`when`<Optional<Boolean>?>(arenaOppfolgingService.kanEnkeltReaktiveres(TEST_FNR)).thenReturn(
            Optional.of<Boolean>(true)
        )
        Mockito.`when`<Boolean?>(kvpService.erUnderKvp(TEST_AKTOR_ID)).thenReturn(false)
        Mockito.`when`<Boolean?>(oppfolgingService.harAktiveTiltaksdeltakelser(TEST_FNR)).thenReturn(false)

        val brukverV2 = EndringPaaOppfolgingsBruker(
            aktorId = TEST_AKTOR_ID,
            fodselsnummer = TEST_FNR.get(),
            formidlingsgruppe = Formidlingsgruppe.ISERV,
            kvalifiseringsgruppe = Kvalifiseringsgruppe.VURDI
        )

        oppfolgingEndringService.oppdaterOppfolgingMedStatusFraArena(brukverV2)

        Mockito.verify(startOppfolgingService, Mockito.never()).startOppfolgingHvisIkkeAlleredeStartet(
            ArgumentMatchers.any(OppfolgingsRegistrering::class.java)
        )
        Mockito.verify(oppfolgingService, Mockito.never())
            .avsluttOppfolging(ArgumentMatchers.any())
    }

    @Test
    fun oppdaterOppfolgingMedStatusFraArena__skal_ikke_avslutte_oppfolging_pa_bruker_som_er_under_kvp() {
        Mockito.`when`<AktorId?>(authService.getAktorIdOrThrow(TEST_FNR)).thenReturn(TEST_AKTOR_ID)
        Mockito.`when`<Optional<OppfolgingEntity>?>(oppfolgingsStatusRepository.hentOppfolging(TEST_AKTOR_ID))
            .thenReturn(
                Optional.of<OppfolgingEntity>(
                    OppfolgingEntity()
                        .setLocalArenaOppfolging(Optional.empty<LocalArenaOppfolging?>())
                        .setUnderOppfolging(true)
                )
            )
        Mockito.`when`<Optional<Boolean>?>(arenaOppfolgingService.kanEnkeltReaktiveres(TEST_FNR)).thenReturn(
            Optional.of<Boolean>(false)
        )
        Mockito.`when`<Boolean?>(kvpService.erUnderKvp(TEST_AKTOR_ID)).thenReturn(true)
        Mockito.`when`<Boolean?>(oppfolgingService.harAktiveTiltaksdeltakelser(TEST_FNR)).thenReturn(false)

        val brukverV2 = EndringPaaOppfolgingsBruker(
            aktorId = TEST_AKTOR_ID,
            fodselsnummer = TEST_FNR.get(),
            formidlingsgruppe = Formidlingsgruppe.ISERV,
            kvalifiseringsgruppe = Kvalifiseringsgruppe.VURDI
        )

        oppfolgingEndringService.oppdaterOppfolgingMedStatusFraArena(brukverV2)

        Mockito.verify(startOppfolgingService, Mockito.never()).startOppfolgingHvisIkkeAlleredeStartet(
            ArgumentMatchers.any(OppfolgingsRegistrering::class.java)
        )
        Mockito.verify(oppfolgingService, Mockito.never())
            .avsluttOppfolging(ArgumentMatchers.any())
    }

    @Test
    fun oppdaterOppfolgingMedStatusFraArena__skal_ikke_avslutte_oppfolging_pa_bruker_som_har_aktive_tiltaksdeltakelser() {
        Mockito.`when`<AktorId?>(authService.getAktorIdOrThrow(TEST_FNR)).thenReturn(TEST_AKTOR_ID)
        Mockito.`when`<Optional<OppfolgingEntity>?>(oppfolgingsStatusRepository.hentOppfolging(TEST_AKTOR_ID))
            .thenReturn(
                Optional.of<OppfolgingEntity>(
                    OppfolgingEntity()
                        .setLocalArenaOppfolging(Optional.empty<LocalArenaOppfolging?>())
                        .setUnderOppfolging(true)
                )
            )
        Mockito.`when`<Optional<Boolean>?>(arenaOppfolgingService.kanEnkeltReaktiveres(TEST_FNR)).thenReturn(
            Optional.of<Boolean>(false)
        )
        Mockito.`when`<Boolean?>(kvpService.erUnderKvp(TEST_AKTOR_ID)).thenReturn(false)
        Mockito.`when`<Boolean?>(oppfolgingService.harAktiveTiltaksdeltakelser(TEST_FNR)).thenReturn(true)

        val brukverV2 = EndringPaaOppfolgingsBruker(
            aktorId = TEST_AKTOR_ID,
            fodselsnummer = TEST_FNR.get(),
            formidlingsgruppe = Formidlingsgruppe.ISERV,
            kvalifiseringsgruppe = Kvalifiseringsgruppe.VURDI
        )

        oppfolgingEndringService.oppdaterOppfolgingMedStatusFraArena(brukverV2)

        Mockito.verify(startOppfolgingService, Mockito.never()).startOppfolgingHvisIkkeAlleredeStartet(
            ArgumentMatchers.any(OppfolgingsRegistrering::class.java)
        )
        Mockito.verify(oppfolgingService, Mockito.never())
            .avsluttOppfolging(ArgumentMatchers.any())
    }
}