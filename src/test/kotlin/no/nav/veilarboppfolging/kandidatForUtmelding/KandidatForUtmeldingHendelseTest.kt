package no.nav.veilarboppfolging.kandidatForUtmelding

import java.time.ZonedDateTime
import java.util.UUID
import no.nav.paw.arbeidssokerregisteret.api.v1.AvsluttetAarsakType
import no.nav.veilarboppfolging.kandidatForUtmelding.dto.KandidatForUtmeldingTag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KandidatForUtmeldingHendelseTest {

    private fun arbeidssøkerPeriodeAvsluttet(
        kandidatForUtmeldingHendelseType: ArbeidssokerperiodeAvsluttetHendelseType,
        aarsakType: AvsluttetAarsakType
    ): ArbeidssøkerPeriodeAvsluttet =
        ArbeidssøkerPeriodeAvsluttet(
            oppfolgingsperiodeUuid = UUID.randomUUID(),
            utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
            utfortAv = "A123123",
            kilde = "test",
            hendelseTidspunkt = ZonedDateTime.now().toInstant(),
            arbeidssokerperiodeAvsluttetHendelseType = kandidatForUtmeldingHendelseType,
            avslutningsarsak = aarsakType.toString()
        )

    @Test
    fun `mapTilTag mapper BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST til riktig tag`() {
        val hendelse = arbeidssøkerPeriodeAvsluttet(ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT, AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST)

        assertThat(hendelse.mapTilTag())
            .isEqualTo(KandidatForUtmeldingTag.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT)
    }

    @Test
    fun `mapTilTag mapper SVARTE_NEI_I_BEKREFTELSE til riktig tag`() {
        val hendelse = arbeidssøkerPeriodeAvsluttet(ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE, AvsluttetAarsakType.SVARTE_NEI_I_BEKREFTELSE)

        assertThat(hendelse.mapTilTag())
            .isEqualTo(KandidatForUtmeldingTag.ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE)
    }

    @Test
    fun `mapTilTag mapper ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET til riktig tag`() {
        val hendelse = arbeidssøkerPeriodeAvsluttet(ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET, AvsluttetAarsakType.UKJENT_VERDI)

        assertThat(hendelse.mapTilTag())
            .isEqualTo(KandidatForUtmeldingTag.ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET)
    }
}