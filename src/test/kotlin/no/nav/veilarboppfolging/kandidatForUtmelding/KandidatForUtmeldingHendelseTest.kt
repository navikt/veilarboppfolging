package no.nav.veilarboppfolging.kandidatForUtmelding

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.AvsluttetAarsakType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class KandidatForUtmeldingHendelseTest {

    private val aktorId = AktorId.of("1234567890")
    private val fnr = Fnr.of("12345678901")

    private fun arbeidssøkerPeriodeAvsluttet(
        avsluttetAv: KandidatForUtmeldingHendelseAvsluttetAv
    ): ArbeidssøkerPeriodeAvsluttet =
        ArbeidssøkerPeriodeAvsluttet(
            aktorId = aktorId,
            fnr = fnr,
            oppfolgingsperiodeUuid = UUID.randomUUID(),
            avsluttetAv = avsluttetAv,
            kilde = "test",
            kandidatForUtmeldingHendelseType = KandidatForUtmeldingHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
            detaljer = AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST.toString()
        )

    @Test
    fun `mapTilTag mapper ARBEIDSSOKERPERIODE_AVSLUTTET avsluttet av BRUKER til riktig tag`() {
        val hendelse = arbeidssøkerPeriodeAvsluttet(KandidatForUtmeldingHendelseAvsluttetAv.BRUKER)

        assertThat(hendelse.mapTilTag())
            .isEqualTo(KandidatForUtmeldingTag.ARBEIDSSOKERPERIODE_AVSLUTTET_BRUKER)
    }

    @Test
    fun `mapTilTag mapper ARBEIDSSOKERPERIODE_AVSLUTTET avsluttet av VEILEDER til riktig tag`() {
        val hendelse = arbeidssøkerPeriodeAvsluttet(KandidatForUtmeldingHendelseAvsluttetAv.VEILEDER)

        assertThat(hendelse.mapTilTag())
            .isEqualTo(KandidatForUtmeldingTag.ARBEIDSSOKERPERIODE_AVSLUTTET_VEILEDER)
    }

    @Test
    fun `mapTilTag mapper ARBEIDSSOKERPERIODE_AVSLUTTET avsluttet av SYSTEM til riktig tag`() {
        val hendelse = arbeidssøkerPeriodeAvsluttet(KandidatForUtmeldingHendelseAvsluttetAv.SYSTEM)

        assertThat(hendelse.mapTilTag())
            .isEqualTo(KandidatForUtmeldingTag.ARBEIDSSOKERPERIODE_AVSLUTTET_SYSTEM)
    }

    @Test
    fun `mapTilTag mapper ARBEIDSSOKERPERIODE_AVSLUTTET avsluttet av UKJENT til riktig tag`() {
        val hendelse = arbeidssøkerPeriodeAvsluttet(KandidatForUtmeldingHendelseAvsluttetAv.UKJENT)

        assertThat(hendelse.mapTilTag())
            .isEqualTo(KandidatForUtmeldingTag.ARBEIDSSOKERPERIODE_AVSLUTTET_UKJENT)
    }
}