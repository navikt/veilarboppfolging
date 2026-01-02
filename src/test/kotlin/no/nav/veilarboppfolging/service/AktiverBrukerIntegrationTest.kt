package no.nav.veilarboppfolging.service

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.EnhetId
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.TilgangType
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.oppfolgingsbruker.AvsluttetAvType
import no.nav.veilarboppfolging.oppfolgingsbruker.StartetAvType
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingStartBegrunnelse
import no.nav.veilarboppfolging.oppfolgingsperioderHendelser.hendelser.OppfolgingStartetHendelseDto
import no.nav.veilarboppfolging.oppfolgingsperioderHendelser.hendelser.OppfolgingsAvsluttetHendelseDto
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class AktiverBrukerIntegrationTest : IntegrationTest() {

    private val FNR: Fnr = Fnr.of("11111111111")
    private val AKTOR_ID: AktorId = AktorId.of("1234523423")

    @Test
    fun `skal lagre oppfolgingstatus på bruker når arbeidsoppfølging er startet`() {
        mockSytemBrukerAuthOk(AKTOR_ID, FNR)
        startOppfolgingSomArbeidsoker(AKTOR_ID, FNR)
        val oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID)
        Assertions.assertThat(oppfolging.isPresent()).isTrue()
    }

    @Test
    fun skalHaandtereAtOppfolgingstatusAlleredeFinnes() {
        mockSytemBrukerAuthOk(AKTOR_ID, FNR)
        oppfolgingsStatusRepository.opprettOppfolging(AKTOR_ID)
        oppfolgingsPeriodeRepository.avsluttSistePeriodeOgAvsluttOppfolging(AKTOR_ID, "veilederid", "begrunnelse")
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
        val veilederIdent = NavIdent("B654321")
        val kontorSattAvVeileder = "4321"
        mockInternBrukerAuthOk(UUID.randomUUID(), AKTOR_ID, FNR, veilederIdent)
        val oppfolgingFør = oppfolgingService.hentOppfolging(AKTOR_ID)
        Assertions.assertThat(oppfolgingFør.isEmpty()).isTrue()

        aktiverBrukerManueltService.aktiverBrukerManuelt(FNR, kontorSattAvVeileder)

        val oppfolging = oppfolgingService.hentOppfolging(AKTOR_ID)
        val nyPeriode = oppfolging.get().oppfolgingsperioder.toList().maxByOrNull { it.startDato }!!
        val lagreteMeldingerIUtboks = getRecordsStoredInKafkaOutbox(kafkaProperties.oppfolgingshendelseV1, FNR.toString())
        assertThat(lagreteMeldingerIUtboks).hasSize(1)
        val hendelse = lagreteMeldingerIUtboks.first() as OppfolgingStartetHendelseDto
        assertThat(hendelse.fnr).isEqualTo(FNR.toString())
        assertThat(hendelse.startetBegrunnelse).isEqualTo(OppfolgingStartBegrunnelse.MANUELL_REGISTRERING_VEILEDER)
        assertThat(hendelse.arenaKontor).isNull()
        assertThat(hendelse.startetAvType).isEqualTo(StartetAvType.VEILEDER)
        assertThat(hendelse.startetAv).isEqualTo(veilederIdent)
        assertThat(hendelse.startetTidspunkt).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(hendelse.foretrukketArbeidsoppfolgingskontor).isEqualTo(kontorSattAvVeileder)
        assertThat(hendelse.oppfolgingsPeriodeId).isEqualTo(nyPeriode.uuid)
    }

    @Test
    fun `Skal lagre OppfolgingStartetHendelse-melding i utboks når oppfølging av arbeidssøkerregistrering startes`() {
        val veilederIdent = NavIdent("B654321")
        mockInternBrukerAuthOk(UUID.randomUUID(), AKTOR_ID, FNR, veilederIdent)
        val oppfolgingFør = oppfolgingService.hentOppfolging(AKTOR_ID)
        assertThat(oppfolgingFør.isEmpty).isTrue()

        startOppfolgingSomArbeidsoker(AKTOR_ID, FNR)

        val lagreteMeldingerIUtboks = getRecordsStoredInKafkaOutbox(kafkaProperties.oppfolgingshendelseV1, FNR.toString())
        assertThat(lagreteMeldingerIUtboks).hasSize(1)
        val hendelse = lagreteMeldingerIUtboks.first() as OppfolgingStartetHendelseDto
        assertThat(hendelse.startetBegrunnelse).isEqualTo(OppfolgingStartBegrunnelse.ARBEIDSSOKER_REGISTRERING)
        assertThat(hendelse.arenaKontor).isNull()
        assertThat(hendelse.foretrukketArbeidsoppfolgingskontor).isNull()
    }

    @Test
    fun `Skal lagre OppfolgingAvsluttetHendelse-melding i utboks når oppfølging avsluttes`() {
        val veilederIdent = NavIdent("B654321")
        val veilederUUID = UUID.randomUUID()
        val enhetId = EnhetId.of("3333")
        val avsluttBegrunnelse = "avslutt begrunnelse"
        mockInternBrukerAuthOk(veilederUUID, AKTOR_ID, FNR, veilederIdent)
        mockPoaoTilgangHarTilgangTilBruker(veilederUUID, FNR, Decision.Permit, TilgangType.SKRIVE)
        mockPoaoTilgangHarTilgangTilEnhet(veilederUUID, enhetId)
        mockVeilarbArenaOppfolgingsBruker(FNR, Formidlingsgruppe.ISERV, oppfolgingsEnhet = enhetId.get())

        startOppfolgingSomArbeidsoker(AKTOR_ID, FNR)
        avsluttOppfolgingManueltSomVeileder(AKTOR_ID, veilederIdent, begrunnelse = avsluttBegrunnelse)

        val lagreteMeldingerIUtboks = getRecordsStoredInKafkaOutbox(kafkaProperties.oppfolgingshendelseV1, FNR.toString())
        assertThat(lagreteMeldingerIUtboks).hasSize(2)
        val startHendelse = lagreteMeldingerIUtboks.first()
        val avsluttetHendelse = lagreteMeldingerIUtboks.last()
        assertInstanceOf<OppfolgingStartetHendelseDto>(startHendelse)
        assertInstanceOf<OppfolgingsAvsluttetHendelseDto>(avsluttetHendelse)
        assertThat(avsluttetHendelse.fnr).isEqualTo(FNR.toString())
        assertThat(avsluttetHendelse.avsluttetAv).isEqualTo(veilederIdent)
        assertThat(avsluttetHendelse.avsluttetAvType).isEqualTo(AvsluttetAvType.VEILEDER)
        assertThat(avsluttetHendelse.startetTidspunkt).isEqualTo(startHendelse.startetTidspunkt)
        assertThat(avsluttetHendelse.avsluttetTidspunkt).isCloseTo(ZonedDateTime.now(), within(1, ChronoUnit.SECONDS))
        assertThat(avsluttetHendelse.oppfolgingsPeriodeId).isEqualTo(startHendelse.oppfolgingsPeriodeId)
    }
}
