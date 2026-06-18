package no.nav.veilarboppfolging.service

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.controller.response.HistorikkHendelse
import no.nav.veilarboppfolging.controller.response.HistorikkHendelseType
import no.nav.veilarboppfolging.oppfolgingsbruker.StartetAvType
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingStartBegrunnelse
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.AvregistreringsType
import no.nav.veilarboppfolging.repository.KvpRepository
import no.nav.veilarboppfolging.repository.OppfolgingsPeriodeRepository
import no.nav.veilarboppfolging.repository.OppfolgingsenhetHistorikkRepository
import no.nav.veilarboppfolging.repository.ReaktiverOppfolgingHendelseEntity
import no.nav.veilarboppfolging.repository.ReaktiveringRepository
import no.nav.veilarboppfolging.repository.VeilederHistorikkRepository
import no.nav.veilarboppfolging.repository.entity.KvpPeriodeEntity
import no.nav.veilarboppfolging.repository.entity.ManuellStatusEntity
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity
import no.nav.veilarboppfolging.repository.enums.KodeverkBruker
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.springframework.aot.hint.TypeReference.listOf
import org.springframework.transaction.support.TransactionTemplate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@RunWith(MockitoJUnitRunner::class)
class HistorikkServiceTest {
    @Mock
    private lateinit var authService: AuthService
    @Mock
    private lateinit var kvpRepositoryMock: KvpRepository
    @Mock
    private lateinit var transactor: TransactionTemplate
    @Mock
    private lateinit var veilederHistorikkRepository: VeilederHistorikkRepository
    @Mock
    private lateinit var oppfolgingsenhetHistorikkRepository: OppfolgingsenhetHistorikkRepository
    @Mock
    private lateinit var manuellStatusService: ManuellStatusService
    @Mock
    private lateinit var oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository
    @InjectMocks
    private lateinit var historikkService: HistorikkService
    @Mock
    private lateinit var reaktiveringRepository: ReaktiveringRepository

    @Before
    fun setup() {
        Mockito.`when`(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID)
        gitt_kvp()
        gitt_manuell_hitsorikk()
        gitt_oppfolging_start_stopp()
    }

    @Test
    fun saksbehandler_har_ikke_tilgang_til_enhet() {
        Mockito.`when`(authService.harTilgangTilEnhet(ENHET)).thenReturn(false)

        val historikk = historikkService.hentInstillingsHistorikk(FNR)
        val begrunnelser = historikk.map { it.begrunnelse }

        Assertions.assertThat(begrunnelser).doesNotContain("IN_KVP")
        Assertions.assertThat(begrunnelser).contains("OUTSIDE_KVP")
    }

    @Test
    fun saksbehandler_har_tilgang_til_enhet() {
        Mockito.`when`<Boolean?>(authService!!.harTilgangTilEnhet(ENHET)).thenReturn(true)

        val historikk = historikkService.hentInstillingsHistorikk(FNR)
        val begrunnelser = historikk.map { obj -> obj.begrunnelse }

        Assertions.assertThat(begrunnelser).contains("IN_KVP", "OUTSIDE_KVP")
    }

    @Test
    fun vis_oppfolgingsperiodehistorikk() {
        Mockito.`when`(authService.harTilgangTilEnhet(ENHET)).thenReturn(true)
        val historikk = historikkService.hentInstillingsHistorikk(FNR)
        val typer = historikk.map { it.type }

        Assertions.assertThat(typer).isEqualTo(
            listOf(
                HistorikkHendelseType.KVP_STARTET,
                HistorikkHendelseType.KVP_STOPPET,
                HistorikkHendelseType.STARTET_OPPFOLGINGSPERIODE,
                HistorikkHendelseType.AVSLUTTET_OPPFOLGINGSPERIODE,
                HistorikkHendelseType.STARTET_OPPFOLGINGSPERIODE,
                HistorikkHendelseType.SATT_TIL_MANUELL,
                HistorikkHendelseType.SATT_TIL_DIGITAL,
                HistorikkHendelseType.SATT_TIL_MANUELL,
                HistorikkHendelseType.SATT_TIL_DIGITAL
            )
        )
    }


    @Test
    fun `oppfolgingsperiodeHistorikk - Veileder (registrert i inngar) - skal vise veileder ident på start og stopp av oppfølgingsperioder i historikken`() {
        Mockito.`when`(authService.harTilgangTilEnhet(ENHET)).thenReturn(true)
        val startetAvVeilder = "Z999999"
        val avsluttetAvVeileder = "Z888888"
        val avsluttetBegrunnelse = "Bruker trenger ikke lenger oppfolging"
        val oppfolgingsPeriode = mockOppfolgingsPeriode(
            startBegrunnelse = OppfolgingStartBegrunnelse.MANUELL_REGISTRERING_VEILEDER,
            startetAv = startetAvVeilder,
            startetAvType = StartetAvType.VEILEDER,
            avsluttetBegrunnelse = avsluttetBegrunnelse,
            avsluttetAv = avsluttetAvVeileder,
        )
        gitt_oppfolgingsperioder(listOf(oppfolgingsPeriode))

        val oppfolgingssEventer = listOf(HistorikkHendelseType.AVSLUTTET_OPPFOLGINGSPERIODE, HistorikkHendelseType.STARTET_OPPFOLGINGSPERIODE)
        val historikk = historikkService.hentInstillingsHistorikk(FNR)
            .filter { oppfolgingssEventer.contains(it.type) }

        Assertions.assertThat(historikk.size).isEqualTo(2)
        val periodeStartetEvent = historikk[0]
        val periodeAvsluttetEvent = historikk[1]

        Assertions.assertThat(periodeStartetEvent).isEqualTo(historikkHendelse(
            type = HistorikkHendelseType.STARTET_OPPFOLGINGSPERIODE,
            tidspunkt = OPPFOLGING_START,
            begrunnelse = "Veileder startet arbeidsrettet oppfølging på bruker",
            opprettetAvType = KodeverkBruker.NAV,
            opprettetAv = startetAvVeilder,
        ))

        Assertions.assertThat(periodeAvsluttetEvent).isEqualTo(historikkHendelse(
            type = HistorikkHendelseType.AVSLUTTET_OPPFOLGINGSPERIODE,
            tidspunkt = OPPFOLGING_END,
            begrunnelse = avsluttetBegrunnelse,
            opprettetAvType = KodeverkBruker.NAV,
            opprettetAv = avsluttetAvVeileder,
        ))
    }

    @Test
    fun `oppfolgingsperiodeHistorikk - Veileder (arbeidssøkerregistrering) - skal vise veileder ident på start og stopp av oppfølgingsperioder i historikken`() {
        Mockito.`when`(authService.harTilgangTilEnhet(ENHET)).thenReturn(true)
        val startetAvVeilder = "Z999999"
        val avsluttetAvVeileder = "Z888888"
        val avsluttetBegrunnelse = "Bruker trenger ikke lenger oppfolging"
        val oppfolgingsPeriode = mockOppfolgingsPeriode(
            startBegrunnelse = OppfolgingStartBegrunnelse.ARBEIDSSOKER_REGISTRERING,
            startetAv = startetAvVeilder,
            startetAvType = StartetAvType.VEILEDER,
            avsluttetBegrunnelse = avsluttetBegrunnelse,
            avsluttetAv = avsluttetAvVeileder,
        )
        gitt_oppfolgingsperioder(listOf(oppfolgingsPeriode))

        val oppfolgingssEventer = listOf(HistorikkHendelseType.AVSLUTTET_OPPFOLGINGSPERIODE, HistorikkHendelseType.STARTET_OPPFOLGINGSPERIODE)
        val historikk = historikkService.hentInstillingsHistorikk(FNR)
            .filter { oppfolgingssEventer.contains(it.type) }

        Assertions.assertThat(historikk.size).isEqualTo(2)
        val periodeStartetEvent = historikk[0]
        val periodeAvsluttetEvent = historikk[1]

        Assertions.assertThat(periodeStartetEvent).isEqualTo(historikkHendelse(
            type = HistorikkHendelseType.STARTET_OPPFOLGINGSPERIODE,
            tidspunkt = OPPFOLGING_START,
            begrunnelse = "Bruker ble registrert som arbeidssøker av veileder",
            opprettetAvType = KodeverkBruker.NAV,
            opprettetAv = startetAvVeilder,
        ))

        Assertions.assertThat(periodeAvsluttetEvent).isEqualTo(historikkHendelse(
            type = HistorikkHendelseType.AVSLUTTET_OPPFOLGINGSPERIODE,
            tidspunkt = OPPFOLGING_END,
            begrunnelse = avsluttetBegrunnelse,
            opprettetAvType = KodeverkBruker.NAV,
            opprettetAv = avsluttetAvVeileder,
        ))
    }


    @Test
    fun `oppfolgingsperiodeHistorikk - System - skal vise veileder ident på start og stopp av oppfølgingsperioder i historikken`() {
        Mockito.`when`(authService.harTilgangTilEnhet(ENHET)).thenReturn(true)
        val avsluttetBegrunnelse = "Bruker trenger ikke lenger oppfolging"
        val oppfolgingsPeriode = mockOppfolgingsPeriode(
            startBegrunnelse = OppfolgingStartBegrunnelse.ARENA_SYNC_ARBS,
            startetAv = null,
            startetAvType = StartetAvType.SYSTEM,
            avsluttetBegrunnelse = avsluttetBegrunnelse,
            avsluttetAv = null,
        )
        gitt_oppfolgingsperioder(listOf(oppfolgingsPeriode))

        val oppfolgingssEventer = listOf(HistorikkHendelseType.AVSLUTTET_OPPFOLGINGSPERIODE, HistorikkHendelseType.STARTET_OPPFOLGINGSPERIODE)
        val historikk = historikkService.hentInstillingsHistorikk(FNR)
            .filter { oppfolgingssEventer.contains(it.type) }

        Assertions.assertThat(historikk.size).isEqualTo(2)
        val periodeStartetEvent = historikk[0]
        val periodeAvsluttetEvent = historikk[1]

        Assertions.assertThat(periodeStartetEvent).isEqualTo(historikkHendelse(
            type = HistorikkHendelseType.STARTET_OPPFOLGINGSPERIODE,
            tidspunkt = OPPFOLGING_START,
            begrunnelse = "Registrert som arbeidssøker i arena",
            opprettetAvType = KodeverkBruker.SYSTEM,
            opprettetAv = null,
        ))

        Assertions.assertThat(periodeAvsluttetEvent).isEqualTo(historikkHendelse(
            type = HistorikkHendelseType.AVSLUTTET_OPPFOLGINGSPERIODE,
            tidspunkt = OPPFOLGING_END,
            begrunnelse = avsluttetBegrunnelse,
            opprettetAvType = KodeverkBruker.SYSTEM,
            opprettetAv = null,
        ))
    }

    @Test
    fun `oppfolgingsperiodeHistorikk - Bruker (arbeidssøkerregistrering) - skal vise veileder ident på start og stopp av oppfølgingsperioder i historikken`() {
        Mockito.`when`(authService.harTilgangTilEnhet(ENHET)).thenReturn(true)
        val avsluttetBegrunnelse = "Bruker trenger ikke lenger oppfolging"

        val oppfolgingsPeriode = mockOppfolgingsPeriode(
            startBegrunnelse = OppfolgingStartBegrunnelse.ARBEIDSSOKER_REGISTRERING,
            startetAv = null,
            startetAvType = StartetAvType.BRUKER,
            avsluttetBegrunnelse = avsluttetBegrunnelse,
            avsluttetAv = null, // Avsluttet av
        )
        gitt_oppfolgingsperioder(listOf(oppfolgingsPeriode))

        val oppfolgingssEventer = listOf(HistorikkHendelseType.AVSLUTTET_OPPFOLGINGSPERIODE, HistorikkHendelseType.STARTET_OPPFOLGINGSPERIODE)
        val historikk = historikkService.hentInstillingsHistorikk(FNR)
            .filter { oppfolgingssEventer.contains(it.type) }

        Assertions.assertThat(historikk.size).isEqualTo(2)
        val periodeStartetEvent = historikk[0]
        val periodeAvsluttetEvent = historikk[1]

        Assertions.assertThat(periodeStartetEvent).isEqualTo(historikkHendelse(
            type = HistorikkHendelseType.STARTET_OPPFOLGINGSPERIODE,
            tidspunkt = OPPFOLGING_START,
            begrunnelse = "Bruker registrerte seg som arbeidssøker",
            opprettetAvType = KodeverkBruker.EKSTERN,
            opprettetAv = null,
        ))

        Assertions.assertThat(periodeAvsluttetEvent).isEqualTo(historikkHendelse(
            type = HistorikkHendelseType.AVSLUTTET_OPPFOLGINGSPERIODE,
            tidspunkt = OPPFOLGING_END,
            begrunnelse = avsluttetBegrunnelse,
            opprettetAvType = KodeverkBruker.SYSTEM,
            opprettetAv = null,
        ))
    }

    @Test
    fun `oppfolgingsperiodeHistorikk - Mangler startet av data - skal vise veileder ident på start og stopp av oppfølgingsperioder i historikken`() {
        Mockito.`when`(authService.harTilgangTilEnhet(ENHET)).thenReturn(true)
        val avsluttetBegrunnelse = "Bruker trenger ikke lenger oppfolging"

        val oppfolgingsPeriode = mockOppfolgingsPeriode(
            startBegrunnelse = null,
            startetAv = null,
            startetAvType = null,
            avsluttetBegrunnelse = avsluttetBegrunnelse,
            avsluttetAv = null, // Avsluttet av
        )
        gitt_oppfolgingsperioder(listOf(oppfolgingsPeriode))

        val oppfolgingssEventer = listOf(HistorikkHendelseType.AVSLUTTET_OPPFOLGINGSPERIODE, HistorikkHendelseType.STARTET_OPPFOLGINGSPERIODE)
        val historikk = historikkService.hentInstillingsHistorikk(FNR)
            .filter { oppfolgingssEventer.contains(it.type) }

        Assertions.assertThat(historikk.size).isEqualTo(2)
        val periodeStartetEvent = historikk[0]
        val periodeAvsluttetEvent = historikk[1]

        Assertions.assertThat(periodeStartetEvent).isEqualTo(historikkHendelse(
            type = HistorikkHendelseType.STARTET_OPPFOLGINGSPERIODE,
            tidspunkt = OPPFOLGING_START,
            begrunnelse = "Startet arbeidsrettet oppfølging på bruker",
            opprettetAvType = null,
            opprettetAv = null,
        ))

        Assertions.assertThat(periodeAvsluttetEvent).isEqualTo(historikkHendelse(
            type = HistorikkHendelseType.AVSLUTTET_OPPFOLGINGSPERIODE,
            tidspunkt = OPPFOLGING_END,
            begrunnelse = avsluttetBegrunnelse,
            opprettetAvType = KodeverkBruker.SYSTEM,
            opprettetAv = null,
        ))
    }

    @Test
    fun `reaktivering av oppfølging vises i historikken`() {
        gitt_oppfolgingsperioder(listOf(mockStartetOppfolgingsperiode()))
        gitt_reaktiveringer(listOf(mockReaktivering()))

        val oppfolgingssEventer = listOf(HistorikkHendelseType.REAKTIVERT_OPPFOLGINGSPERIODE, HistorikkHendelseType.STARTET_OPPFOLGINGSPERIODE)
        val historikk = historikkService.hentInstillingsHistorikk(FNR)
            .filter { oppfolgingssEventer.contains(it.type) }

        Assertions.assertThat(historikk.size).isEqualTo(2)
        val periodeStartetEvent = historikk[0]
        val periodeReaktivertEvent = historikk[1]

        Assertions.assertThat(periodeStartetEvent).isEqualTo(historikkHendelse(
            type = HistorikkHendelseType.STARTET_OPPFOLGINGSPERIODE,
            tidspunkt = OPPFOLGING_START,
            begrunnelse = "Startet arbeidsrettet oppfølging på bruker",
            opprettetAvType = KodeverkBruker.NAV,
            opprettetAv = "defaultVeileder",
        ))

        Assertions.assertThat(periodeReaktivertEvent).isEqualTo(historikkHendelse(
            type = HistorikkHendelseType.REAKTIVERT_OPPFOLGINGSPERIODE,
            tidspunkt = OPPFOLGING_REAKTIVERT,
            begrunnelse = "Bruker manuelt reaktivert i Arena av veileder",
            opprettetAvType = KodeverkBruker.NAV,
            opprettetAv = "veileder som reaktiverte",
        ))
    }

    private fun gitt_oppfolgingsperioder(oppfolgingsPerioder: List<OppfolgingsperiodeEntity>) {
        Mockito.`when`(
            oppfolgingsPeriodeRepository.hentOppfolgingsperioder(AKTOR_ID)
        ).thenReturn(oppfolgingsPerioder)
    }

    private fun gitt_reaktiveringer(reaktiveringer: List<ReaktiverOppfolgingHendelseEntity>) {
        Mockito.`when`(
            reaktiveringRepository.hentReaktiveringer(AKTOR_ID)
        ).thenReturn(reaktiveringer)
    }

    private fun gitt_oppfolging_start_stopp(startetAvVeilder: String? = null, avsluttetAvVeileder: String? = null) {
        val oppfolgingsperiodeEntities = listOf(
            OppfolgingsperiodeEntity(
                UUID.randomUUID(),
        AKTOR_ID.get(),
                avsluttetAvVeileder,
                        OPPFOLGING_START,
                OPPFOLGING_END,
                "Avsluttet manuelt",
                emptyList(),
                OppfolgingStartBegrunnelse.MANUELL_REGISTRERING_VEILEDER,
                startetAvVeilder,
                null,
                AvregistreringsType.ManuellAvregistrering
            ),
            OppfolgingsperiodeEntity(
                UUID.randomUUID(),
                AKTOR_ID.get(),
                null,
                OPPFOLGING_END,
                null,
                null,
                emptyList(),
                OppfolgingStartBegrunnelse.MANUELL_REGISTRERING_VEILEDER,
                null,
                null,
                null
            )
        )
        Mockito.`when`(
            oppfolgingsPeriodeRepository.hentOppfolgingsperioder(AKTOR_ID)
        ).thenReturn(oppfolgingsperiodeEntities)
    }


    private fun gitt_manuell_hitsorikk() {
        val manuellStatus = listOf(
            ManuellStatusEntity(
                null,
                AKTOR_ID.get(),
                true,
                BEFORE_KVP,
                "OUTSIDE_KVP",
                null,
                null,
            ),
            ManuellStatusEntity(
                null,
                AKTOR_ID.get(),
                false,
                BEFORE_KVP,
                "OUTSIDE_KVP",
                null,
                null,
            ),
            ManuellStatusEntity(
                null,
                AKTOR_ID.get(),
                true,
                IN_KVP,
                "IN_KVP",
                null,
                null,
            ),
            ManuellStatusEntity(
                null,
                AKTOR_ID.get(),
                false,
                IN_KVP,
                "IN_KVP",
                null,
                null,
            ),
            ManuellStatusEntity(
                null,
                AKTOR_ID.get(),
                true,
                AFTER_KVP,
                "OUTSIDE_KVP",
                null,
                null,
            ),
            ManuellStatusEntity(
                null,
                AKTOR_ID.get(),
                false,
                AFTER_KVP,
                "OUTSIDE_KVP",
                null,
                null,
            )
        )
        Mockito.`when`(manuellStatusService.hentManuellStatusHistorikk(AKTOR_ID)).thenReturn(manuellStatus)
    }



    private fun gitt_kvp() {
        val kvp = mutableListOf<KvpPeriodeEntity?>(
            KvpPeriodeEntity(
                kvpId = 1L,
                aktorId = AKTOR_ID.get(),
                opprettetDato = KVP_START,
                opprettetBegrunnelse = "IN_KVP",
                avsluttetDato = KVP_STOP,
                avsluttetBegrunnelse = "IN_KVP",
                enhet = ENHET,
                serial = null,
                opprettetAv = null,
            )
        )
        Mockito.`when`(kvpRepositoryMock.hentKvpHistorikk(AKTOR_ID)).thenReturn(kvp)
    }

    private fun mockOppfolgingsPeriode(
        startBegrunnelse: OppfolgingStartBegrunnelse?,
        startetAv: String?,
        startetAvType: StartetAvType?,
        avsluttetBegrunnelse: String? = null,
        avsluttetAv: String? = null,
        avregistreringsType: AvregistreringsType? = null,
    ): OppfolgingsperiodeEntity {
        return OppfolgingsperiodeEntity(
            UUID.randomUUID(),
            AKTOR_ID.get(),
            avsluttetAv,
            OPPFOLGING_START,
            OPPFOLGING_END,
            avsluttetBegrunnelse,
            emptyList(),
            startBegrunnelse,
            startetAv,
            startetAvType,
            avregistreringsType
        )
    }

    private fun mockStartetOppfolgingsperiode(): OppfolgingsperiodeEntity {
        return OppfolgingsperiodeEntity(
            UUID.randomUUID(),
            AKTOR_ID.get(),
            null,
            OPPFOLGING_START,
            null,
            null,
            emptyList(),
            null,
            "defaultVeileder",
            StartetAvType.VEILEDER,
            null,
        )
    }

    private fun mockReaktivering(): ReaktiverOppfolgingHendelseEntity {
        return ReaktiverOppfolgingHendelseEntity(
            AKTOR_ID.get(),
            OPPFOLGING_REAKTIVERT,
            "veileder som reaktiverte"
        )
    }

    fun historikkHendelse(
        type: HistorikkHendelseType,
        tidspunkt: ZonedDateTime,
        begrunnelse: String,
        opprettetAvType: KodeverkBruker?,
        opprettetAv: String?
    ): HistorikkHendelse {
        return HistorikkHendelse(
            type,
            tidspunkt,
            begrunnelse,
            opprettetAvType,
            opprettetAv,
            null,
            null,
            null
        )
    }

    companion object {
        private val FNR: Fnr = Fnr.of("fnr")
        private val AKTOR_ID: AktorId = AktorId.of("aktorId")
        private const val ENHET = "6767"

        private val OPPFOLGING_START: ZonedDateTime = ZonedDateTime.now().minusDays(10)
        private val OPPFOLGING_END: ZonedDateTime = OPPFOLGING_START.plusDays(5)
        private val OPPFOLGING_REAKTIVERT: ZonedDateTime = OPPFOLGING_START.plusDays(3)
        private val BEFORE_KVP: ZonedDateTime = ZonedDateTime.now()
        private val ALSO_BEFORE_KVP: ZonedDateTime = BEFORE_KVP.plus(1, ChronoUnit.HOURS)
        private val KVP_START: ZonedDateTime = BEFORE_KVP.plus(1, ChronoUnit.DAYS)
        private val IN_KVP: ZonedDateTime = BEFORE_KVP.plus(2, ChronoUnit.DAYS)
        private val KVP_STOP: ZonedDateTime = BEFORE_KVP.plus(3, ChronoUnit.DAYS)
        private val AFTER_KVP: ZonedDateTime = BEFORE_KVP.plus(4, ChronoUnit.DAYS)
    }
}