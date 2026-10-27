package no.nav.veilarboppfolging.kafka

import java.time.Instant
import java.time.LocalDate
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.person.pdl.leesah.Endringstype
import no.nav.person.pdl.leesah.Personhendelse
import no.nav.person.pdl.leesah.doedsfall.Doedsfall
import no.nav.person.pdl.leesah.falskidentitet.FalskIdentitet
import no.nav.person.pdl.leesah.statsborgerskap.Statsborgerskap
import no.nav.person.pdl.leesah.utflytting.UtflyttingFraNorge
import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.veilarboppfolging.IntegrationTest
import no.nav.veilarboppfolging.client.pdl.ForenkletFolkeregisterStatus
import no.nav.veilarboppfolging.client.pdl.FregStatusOgStatsborgerskap
import no.nav.veilarboppfolging.kandidatForUtmelding.dto.KandidatForUtmeldingTagDto
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersonhendelseConsumerTest(
    @Autowired
    private val personhendelseConsumer: PersonhendelseConsumer,
) : IntegrationTest() {
    private val fnr = Fnr.of("01010198765")
    private val aktorId = AktorId.of("2915972563113")

    @Test
    fun `ikke dødsfall-hendelse - blir ignorert`() {
        val personhendelse = Personhendelse.newBuilder()
            .setHendelseId("dummyHendelseId")
            .setPersonidenter(listOf("2915972563113"))
            .setMaster("PDL")
            .setOpprettet(Instant.now())
            .setOpplysningstype("FalskIdentitetV1")
            .setEndringstype(Endringstype.OPPRETTET)
            .setFalskIdentitet(FalskIdentitet(true))
            .build()
        val melding = ConsumerRecord("topic", 0, 0, "key", personhendelse)

        personhendelseConsumer.consumePersonhendelse(melding)
    }

    @Test
    fun `dødsfall-hendelse, bruker er ikke under oppfølging - blir ignorert`() {
        val melding = ConsumerRecord("topic", 0, 0, "key", personhendelseForDodsfall(listOf(fnr.get(), aktorId.get())))

        personhendelseConsumer.consumePersonhendelse(melding)
    }

    @Test
    fun `dødsfall-hendelse, bruker er under oppfølging og kan avsluttes - oppfølging blir avsluttet`() {
        mockSytemBrukerAuthOk(aktorId, fnr)
        startOppfolgingSomArbeidsoker(aktorId, fnr)
        setLocalArenaOppfolging(aktorId, Formidlingsgruppe.ARBS)
        mockTiltakshistorikk(fnr, harAktiveDeltakelser = false)
        mockUngdomsprogram(fnr, erDeltaker = false)
        mockAap(fnr, harAap = false)
        assertThat(oppfolgingService.erUnderOppfolging(aktorId)).isTrue

        val melding = ConsumerRecord("topic", 0, 0, "key", personhendelseForDodsfall(listOf(fnr.get(), aktorId.get())))

        personhendelseConsumer.consumePersonhendelse(melding)

        assertThat(oppfolgingService.erUnderOppfolging(aktorId)).isFalse
    }

    @Test
    fun `dødsfall-hendelse, bruker er under oppfølging og kan ikke avsluttes - oppfølging blir avsluttet uansett`() {
        mockSytemBrukerAuthOk(aktorId, fnr)
        startOppfolgingSomArbeidsoker(aktorId, fnr)
        setLocalArenaOppfolging(aktorId, Formidlingsgruppe.ARBS)
        mockTiltakshistorikk(fnr, harAktiveDeltakelser = false)
        mockUngdomsprogram(fnr, erDeltaker = false)
        mockAap(fnr, harAap = true)
        assertThat(oppfolgingService.erUnderOppfolging(aktorId)).isTrue

        val melding = ConsumerRecord("topic", 0, 0, "key", personhendelseForDodsfall(listOf(fnr.get(), aktorId.get())))

        personhendelseConsumer.consumePersonhendelse(melding)

        assertThat(oppfolgingService.erUnderOppfolging(aktorId)).isFalse
    }

    @Test
    fun `utflyttet-hendelse, bruker er under oppfølging, har ikke lovlig opphold, kan avsluttes - lagrer utmeldingskandidat`() {
        mockSytemBrukerAuthOk(aktorId, fnr)
        startOppfolgingSomArbeidsoker(aktorId, fnr)
        setLocalArenaOppfolging(aktorId, Formidlingsgruppe.ARBS)
        mockTiltakshistorikk(fnr, harAktiveDeltakelser = false)
        mockUngdomsprogram(fnr, erDeltaker = false)
        mockAap(fnr, harAap = false)
        mockPdlFolkeregisterStatus(fnr, FregStatusOgStatsborgerskap(
            fregStatus = ForenkletFolkeregisterStatus.opphoert,
            statsborgerskap = listOf("USA"),
            under18 = false,
        ))
        assertThat(oppfolgingService.erUnderOppfolging(aktorId)).isTrue

        val melding = ConsumerRecord("topic", 0, 0, "key", personhendelseForUtflytting(listOf(fnr.get(), aktorId.get())))

        personhendelseConsumer.consumePersonhendelse(melding)

        assertThat(oppfolgingService.erUnderOppfolging(aktorId)).isTrue
        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId)).isEqualTo(KandidatForUtmeldingTagDto.FOLKEREGISTERSTATUS_OPPHORT)
    }

    @Test
    fun `statsborgerskap-hendelse, bruker er under oppfølging, har ikke lovlig opphold, kan avsluttes - lagrer utmeldingskandidat`() {
        mockSytemBrukerAuthOk(aktorId, fnr)
        startOppfolgingSomArbeidsoker(aktorId, fnr)
        setLocalArenaOppfolging(aktorId, Formidlingsgruppe.ARBS)
        mockTiltakshistorikk(fnr, harAktiveDeltakelser = false)
        mockUngdomsprogram(fnr, erDeltaker = false)
        mockAap(fnr, harAap = false)
        mockPdlFolkeregisterStatus(fnr, FregStatusOgStatsborgerskap(
            fregStatus = ForenkletFolkeregisterStatus.opphoert,
            statsborgerskap = listOf("USA"),
            under18 = false,
        ))
        assertThat(oppfolgingService.erUnderOppfolging(aktorId)).isTrue

        val melding = ConsumerRecord("topic", 0, 0, "key", personhendelseForStatsborgerskap(listOf(fnr.get(), aktorId.get())))

        personhendelseConsumer.consumePersonhendelse(melding)

        assertThat(oppfolgingService.erUnderOppfolging(aktorId)).isTrue
        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId)).isEqualTo(KandidatForUtmeldingTagDto.FOLKEREGISTERSTATUS_OPPHORT)
    }

    @Test
    fun `opphørt bostedsadresse-hendelse, bruker er under oppfølging, har ikke lovlig opphold, kan avsluttes - lagrer utmeldingskandidat`() {
        mockSytemBrukerAuthOk(aktorId, fnr)
        startOppfolgingSomArbeidsoker(aktorId, fnr)
        setLocalArenaOppfolging(aktorId, Formidlingsgruppe.ARBS)
        mockTiltakshistorikk(fnr, harAktiveDeltakelser = false)
        mockUngdomsprogram(fnr, erDeltaker = false)
        mockAap(fnr, harAap = false)
        mockPdlFolkeregisterStatus(fnr, FregStatusOgStatsborgerskap(
            fregStatus = ForenkletFolkeregisterStatus.opphoert,
            statsborgerskap = listOf("USA"),
            under18 = false,
        ))
        assertThat(oppfolgingService.erUnderOppfolging(aktorId)).isTrue

        val melding = ConsumerRecord("topic", 0, 0, "key", personhendelseForOpphortBostedsadresse(listOf(fnr.get(), aktorId.get())))

        personhendelseConsumer.consumePersonhendelse(melding)

        assertThat(oppfolgingService.erUnderOppfolging(aktorId)).isTrue
        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId)).isEqualTo(KandidatForUtmeldingTagDto.FOLKEREGISTERSTATUS_OPPHORT)
    }

    @Test
    fun `statsborgerskap-hendelse, bruker er under oppfølging, har lovlig opphold - lagrer ikke utmeldingskandidat`() {
        mockSytemBrukerAuthOk(aktorId, fnr)
        startOppfolgingSomArbeidsoker(aktorId, fnr)
        setLocalArenaOppfolging(aktorId, Formidlingsgruppe.ARBS)
        mockTiltakshistorikk(fnr, harAktiveDeltakelser = false)
        mockUngdomsprogram(fnr, erDeltaker = false)
        mockAap(fnr, harAap = false)
        mockPdlFolkeregisterStatus(fnr, FregStatusOgStatsborgerskap(
            fregStatus = ForenkletFolkeregisterStatus.dNummer,
            statsborgerskap = listOf("SWE"),
            under18 = false,
        ))
        assertThat(oppfolgingService.erUnderOppfolging(aktorId)).isTrue

        val melding = ConsumerRecord("topic", 0, 0, "key", personhendelseForStatsborgerskap(listOf(fnr.get(), aktorId.get())))

        personhendelseConsumer.consumePersonhendelse(melding)

        assertThat(oppfolgingService.erUnderOppfolging(aktorId)).isTrue
        assertThat(kandidatForUtmeldingService.hentKandidatForUtmeldingTag(aktorId)).isNull()
    }

    private fun personhendelseForDodsfall(identer: List<String>): Personhendelse {
        return Personhendelse.newBuilder()
            .setHendelseId("dummyHendelseId")
            .setPersonidenter(identer)
            .setMaster("PDL")
            .setOpprettet(Instant.now())
            .setOpplysningstype("DoedsfallV1")
            .setEndringstype(Endringstype.OPPRETTET)
            .setDoedsfall(Doedsfall(LocalDate.now()))
            .build()
    }

    private fun personhendelseForUtflytting(identer: List<String>): Personhendelse {
        return Personhendelse.newBuilder()
            .setHendelseId("dummyHendelseId")
            .setPersonidenter(identer)
            .setMaster("PDL")
            .setOpprettet(Instant.now())
            .setOpplysningstype("UtflyttingV1")
            .setEndringstype(Endringstype.OPPRETTET)
            .setUtflyttingFraNorge(UtflyttingFraNorge("USA", "tilflyttingssted", LocalDate.now()))
            .build()
    }

    private fun personhendelseForStatsborgerskap(identer: List<String>): Personhendelse {
        return Personhendelse.newBuilder()
            .setHendelseId("dummyHendelseId")
            .setPersonidenter(identer)
            .setMaster("PDL")
            .setOpprettet(Instant.now())
            .setOpplysningstype("StatsborgerskapV1")
            .setEndringstype(Endringstype.OPPRETTET)
            .setStatsborgerskap(Statsborgerskap("USA", LocalDate.now(), LocalDate.now(), LocalDate.now()))
            .build()
    }

    private fun personhendelseForOpphortBostedsadresse(identer: List<String>): Personhendelse {
        return Personhendelse.newBuilder()
            .setHendelseId("dummyHendelseId")
            .setPersonidenter(identer)
            .setMaster("PDL")
            .setOpprettet(Instant.now())
            .setOpplysningstype("BostedsadresseV1")
            .setEndringstype(Endringstype.OPPHOERT)
            .build()
    }
}