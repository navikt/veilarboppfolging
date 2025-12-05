package no.nav.veilarboppfolging.service

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.kafka.json.topic.onprem.EndringPaaOppfoelgingsBrukerV2
import no.nav.veilarboppfolging.LocalDatabaseSingleton
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.EndringPaaOppfolgingsBruker
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository
import no.nav.veilarboppfolging.repository.OppfolgingsenhetHistorikkRepository
import no.nav.veilarboppfolging.test.DbTestUtils
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.ZonedDateTime

class OppfolgingsenhetEndringServiceTest {
    private val authService: AuthService = Mockito.mock<AuthService>(AuthService::class.java)

    private val repo =
        OppfolgingsenhetHistorikkRepository(NamedParameterJdbcTemplate(LocalDatabaseSingleton.jdbcTemplate))
    private val oppfolgingsStatusRepository =
        OppfolgingsStatusRepository(NamedParameterJdbcTemplate(LocalDatabaseSingleton.jdbcTemplate))
    private val oppfolgingsenhetEndringService = OppfolgingsenhetEndringService(repo, oppfolgingsStatusRepository)

    @Before
    fun cleanup() {
        DbTestUtils.cleanupTestDb()
    }

    @Test
    fun skal_lik() {
        MatcherAssert.assertThat<Boolean?>(EnhetId.of("1234") == EnhetId.of("1234"), Matchers.equalTo<Boolean?>(true))
        MatcherAssert.assertThat<Boolean?>(EnhetId.of("1234") == EnhetId.of("1233"), Matchers.equalTo<Boolean?>(false))
    }

    @Test
    fun skal_legge_til_ny_enhet_i_historikk_gitt_eksisterende_historikk() {
        Mockito.`when`<AktorId?>(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID)

        gitt_eksisterende_historikk(NYTT_NAV_KONTOR)
        behandle_ny_enhets_endring(EnhetId.of("2222"))

        val historikk = repo.hentOppfolgingsenhetEndringerForAktorId(AKTOR_ID)

        MatcherAssert.assertThat<Int?>(historikk.size, Matchers.`is`<Int?>(2))
        MatcherAssert.assertThat<String?>(historikk.get(0)!!.getEnhet(), Matchers.equalTo<String?>("2222"))
        MatcherAssert.assertThat<String?>(historikk.get(1)!!.getEnhet(), Matchers.equalTo<String?>("1111"))
    }

    @Test
    fun skal_legge_til_ny_enhet_i_historikk_gitt_tom_historikk() {
        Mockito.`when`<AktorId?>(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID)

        behandle_ny_enhets_endring(NYTT_NAV_KONTOR)
        val historikk = repo.hentOppfolgingsenhetEndringerForAktorId(AKTOR_ID)

        MatcherAssert.assertThat<Int?>(historikk.size, Matchers.`is`<Int?>(1))
        MatcherAssert.assertThat<String?>(
            historikk.get(0)!!.getEnhet(),
            Matchers.equalTo<String?>(NYTT_NAV_KONTOR.get())
        )
    }

    @Test
    fun skal_ikke_legge_til_ny_enhet_i_historikk_hvis_samme_enhet_allerede_er_nyeste_historikk() {
        Mockito.`when`<AktorId?>(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID)

        gitt_eksisterende_historikk(NYTT_NAV_KONTOR)
        behandle_ny_enhets_endring(NYTT_NAV_KONTOR)

        val historikk = repo.hentOppfolgingsenhetEndringerForAktorId(AKTOR_ID)

        MatcherAssert.assertThat<Int?>(historikk.size, Matchers.`is`<Int?>(1))
        MatcherAssert.assertThat<String?>(
            historikk.get(0)!!.getEnhet(),
            Matchers.equalTo<String?>(NYTT_NAV_KONTOR.get())
        )
    }

    @Test
    fun skal_legge_til_ny_enhet_med_samme_enhet_midt_i_historikken() {
        Mockito.`when`<AktorId?>(authService.getAktorIdOrThrow(FNR)).thenReturn(AKTOR_ID)

        gitt_eksisterende_historikk(EnhetId.of("1234"))
        gitt_eksisterende_historikk(NYTT_NAV_KONTOR)
        gitt_eksisterende_historikk(EnhetId.of("4321"))

        behandle_ny_enhets_endring(NYTT_NAV_KONTOR)

        val historikk = repo.hentOppfolgingsenhetEndringerForAktorId(AKTOR_ID)

        MatcherAssert.assertThat<Int?>(historikk.size, Matchers.`is`<Int?>(4))
        MatcherAssert.assertThat<String?>(
            historikk.get(0)!!.getEnhet(),
            Matchers.equalTo<String?>(NYTT_NAV_KONTOR.get())
        )
    }


    private fun behandle_ny_enhets_endring(navKontor: EnhetId) {
        val arenaEndring = EndringPaaOppfolgingsBruker(
            aktorId = AKTOR_ID,
            fodselsnummer = FNR.get(),
            oppfolgingsenhet = navKontor.get(),
            formidlingsgruppe = Formidlingsgruppe.ARBS,
            sistEndretDato = ZonedDateTime.now()
        )

        oppfolgingsenhetEndringService.behandleBrukerEndring(arenaEndring)
    }

    private fun gitt_eksisterende_historikk(navKontor: EnhetId) {
        repo.insertOppfolgingsenhetEndringForAktorId(AKTOR_ID, navKontor)
    }

    companion object {
        private val FNR: Fnr = Fnr.of("12356")
        private val AKTOR_ID: AktorId = AktorId.of("123")
        private val NYTT_NAV_KONTOR: EnhetId = EnhetId.of("1111")
    }
}