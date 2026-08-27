package no.nav.veilarboppfolging.kandidatForUtmelding

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID
import no.nav.paw.arbeidssokerregisteret.api.v1.AvsluttetAarsakType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KandidatForUtmeldingHendelseTest {

    private fun arbeidssøkerPeriodeAvsluttet(
        kandidatForUtmeldingHendelseType: ArbeidssokerperiodeAvsluttetHendelseType,
        aarsakType: AvsluttetAarsakType,
        hendelseTidspunkt: Instant = ZonedDateTime.now().toInstant(),
    ): ArbeidssøkerPeriodeAvsluttet =
        ArbeidssøkerPeriodeAvsluttet(
            oppfolgingsperiodeUuid = UUID.randomUUID(),
            utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
            utfortAv = "A123123",
            kilde = "test",
            hendelseTidspunkt = hendelseTidspunkt,
            arbeidssokerperiodeAvsluttetHendelseType = kandidatForUtmeldingHendelseType,
            avslutningsarsak = aarsakType.toString()
        )

    private fun forlengelseHendelse(
        forlengelseHendelseType: ForlengelseHendelseType,
        hendelseTidspunkt: Instant = ZonedDateTime.now().toInstant(),
        forlengetTil: LocalDate? = LocalDate.now().plusDays(14),
    ): ForlengelseHendelse =
        ForlengelseHendelse(
            oppfolgingsperiodeUuid = UUID.randomUUID(),
            utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.VEILEDER,
            utfortAv = "A123123",
            kilde = "test",
            forlengelseHendelseType = forlengelseHendelseType,
            hendelseTidspunkt = hendelseTidspunkt,
            forlengetTil = forlengetTil,
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

    @Test
    fun `beregnAvsluttesAutomatiskDato - settes til 28 dager etter hendelsestidspunkt når kandidat opprettes forste gang`() {
        val hendelseTidspunkt = ZonedDateTime.now().toInstant()
        val hendelse = arbeidssøkerPeriodeAvsluttet(
            ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
            AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST,
            hendelseTidspunkt = hendelseTidspunkt,
        )

        val forventetDato = LocalDateTime.ofInstant(hendelseTidspunkt, ZoneOffset.UTC)
            .plusDays(KandidatForUtmeldingHendelse.KARENSTID_DAGER)

        assertThat(hendelse.beregnAvsluttesAutomatiskDato()).isEqualTo(forventetDato)
    }

    @Test
    fun `beregnAvsluttesAutomatiskDato - er null nar forlengelse opprettes`() {
        val hendelse = forlengelseHendelse(ForlengelseHendelseType.FORLENGELSE_OPPRETTET)

        assertThat(hendelse.beregnAvsluttesAutomatiskDato()).isNull()
    }

    @Test
    fun `beregnAvsluttesAutomatiskDato - er null nar forlengelse endres`() {
        val hendelse = forlengelseHendelse(ForlengelseHendelseType.FORLENGELSE_ENDRET)

        assertThat(hendelse.beregnAvsluttesAutomatiskDato()).isNull()
    }

    @Test
    fun `beregnAvsluttesAutomatiskDato - settes til 28 dager etter utlopstidspunkt nar forlengelse utloper`() {
        val hendelseTidspunkt = ZonedDateTime.now().toInstant()
        val hendelse = forlengelseHendelse(
            ForlengelseHendelseType.FORLENGELSE_UTLOPT,
            hendelseTidspunkt = hendelseTidspunkt,
            forlengetTil = null,
        )

        val forventetDato = LocalDateTime.ofInstant(hendelseTidspunkt, ZoneOffset.UTC)
            .plusDays(KandidatForUtmeldingHendelse.KARENSTID_DAGER)

        assertThat(hendelse.beregnAvsluttesAutomatiskDato()).isEqualTo(forventetDato)
    }
}
