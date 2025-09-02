package no.nav.veilarboppfolging.service

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.oppfolgingsperioderHendelser.hendelser.OppfolgingStartetHendelseDto
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class AktiverBrukerIntegrationTest : IntegrationTest() {

    private val FNR: Fnr = Fnr.of("11111111111")
    private val AKTOR_ID: AktorId = AktorId.of("1234523423")

    @Test
    fun skalLagreIDatabaseDersomKallTilArenaErOK() {
        mockAuthOk(AKTOR_ID, FNR)
        startOppfolgingSomArbeidsoker(AKTOR_ID, FNR)
        val oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID)
        Assertions.assertThat(oppfolging.isPresent()).isTrue()
    }

    @Test
    fun skalHaandtereAtOppfolgingstatusAlleredeFinnes() {
        mockAuthOk(AKTOR_ID, FNR)
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID)
        oppfolgingsPeriodeRepository.avslutt(AKTOR_ID, "veilederid", "begrunnelse")
        startOppfolgingSomArbeidsoker(AKTOR_ID, FNR)
        val oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID)
        Assertions.assertThat(oppfolging.get().isUnderOppfolging()).isTrue()
    }

    @Test
    fun aktiver_sykmeldt_skal_starte_oppfolging() {
        mockInternBrukerAuthOk(UUID.randomUUID(), AKTOR_ID, FNR)
        val oppfolgingFør = oppfolgingService.hentOppfolging(AKTOR_ID)
        Assertions.assertThat(oppfolgingFør.isEmpty()).isTrue()
        aktiverBrukerManueltService.aktiverBrukerManuelt(FNR, "1234")
        val oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID)
        Assertions.assertThat(oppfolging.get().isUnderOppfolging()).isTrue()
    }

    @Test
    fun `Skal lagre OppfolgingStartetHendelse-melding i utboks når oppfølging startes manuelt`() {
        val veilederIdent = "B654321"
        val kontorSattAvVeileder = "4321"
        mockInternBrukerAuthOk(UUID.randomUUID(), AKTOR_ID, FNR, veilederIdent)
        val oppfolgingFør = oppfolgingService.hentOppfolging(AKTOR_ID)
        Assertions.assertThat(oppfolgingFør.isEmpty()).isTrue()

        aktiverBrukerManueltService.aktiverBrukerManuelt(FNR, kontorSattAvVeileder)

        val oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID)
        val nyPeriode = oppfolging.get().oppfolgingsperioder.toList().maxByOrNull { it.startDato }!!
        val lagreteMeldingerIUtboks = getSavedRecord(kafkaProperties.oppfolgingsperiodehendelseV1, FNR.toString())
        assertThat(lagreteMeldingerIUtboks).hasSize(1)
        assertThat(lagreteMeldingerIUtboks.first().fnr).isEqualTo(FNR.toString())
        assertThat(lagreteMeldingerIUtboks.first().startetBegrunnelse).isEqualTo("MANUELL_REGISTRERING_VEILEDER")
        assertThat(lagreteMeldingerIUtboks.first().arenaKontor).isNull()
        assertThat(lagreteMeldingerIUtboks.first().startetAvType).isEqualTo("VEILEDER")
        assertThat(lagreteMeldingerIUtboks.first().startetAv).isEqualTo(veilederIdent)
        assertThat(lagreteMeldingerIUtboks.first().startetTidspunkt).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(lagreteMeldingerIUtboks.first().arbeidsoppfolgingsKontorSattAvVeileder).isEqualTo(kontorSattAvVeileder)
        assertThat(lagreteMeldingerIUtboks.first().oppfolgingsPeriodeId).isEqualTo(nyPeriode.uuid)
    }

    private val objectMapper = JsonUtils.getMapper().also {
        it.registerKotlinModule()
    }

    private fun getSavedRecord(topic: String, fnr: String): List<OppfolgingStartetHendelseDto> {
        return namedParameterJdbcTemplate.query("""
            SELECT * FROM kafka_producer_record
            where topic = :topic and key = :fnr
        """.trimIndent(), mapOf("topic" to topic, "fnr" to fnr.toByteArray())) { resultSet, row ->
            val json = resultSet.getBytes("value").toString(Charsets.UTF_8)
            objectMapper.readValue(json, OppfolgingStartetHendelseDto::class.java)
        }
    }
}
