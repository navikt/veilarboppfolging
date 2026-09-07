package no.nav.veilarboppfolging.kandidatForUtmelding

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID
import no.nav.common.types.identer.Fnr
import no.nav.paw.arbeidssokerregisteret.api.v1.AvsluttetAarsakType
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmelding.Companion.KARENSTID_DAGER
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Operasjon
import no.nav.veilarboppfolging.kandidatForUtmelding.dto.KandidatForUtmeldingTagDto
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
            .isEqualTo(KandidatForUtmeldingTagDto.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT)
    }

    @Test
    fun `mapTilTag mapper SVARTE_NEI_I_BEKREFTELSE til riktig tag`() {
        val hendelse = arbeidssøkerPeriodeAvsluttet(ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE, AvsluttetAarsakType.SVARTE_NEI_I_BEKREFTELSE)

        assertThat(hendelse.mapTilTag())
            .isEqualTo(KandidatForUtmeldingTagDto.ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE)
    }

    @Test
    fun `mapTilTag mapper ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET til riktig tag`() {
        val hendelse = arbeidssøkerPeriodeAvsluttet(ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET, AvsluttetAarsakType.UKJENT_VERDI)

        assertThat(hendelse.mapTilTag())
            .isEqualTo(KandidatForUtmeldingTagDto.ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET)
    }

    @Test
    fun `beregnAvsluttesAutomatiskDato - settes til 28 dager etter hendelsestidspunkt når kandidat opprettes forste gang`() {
        val hendelseTidspunkt = ZonedDateTime.now().toInstant()
        val hendelse = arbeidssøkerPeriodeAvsluttet(
            ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
            AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST,
            hendelseTidspunkt = hendelseTidspunkt,
        ).let { KandidatForUtmelding.fromHendelse(it) as AktivKandidatForUtmelding }
        val forventetDato = LocalDateTime.ofInstant(hendelseTidspunkt, ZoneOffset.UTC)
            .plusDays(KARENSTID_DAGER)

        val automatiskAvslutningDato = hendelse.avsluttesAutomatiskDato

        assertThat(automatiskAvslutningDato).isEqualTo(forventetDato)
        assertThat(hendelse.sisteHendelse.tilFilterhendelseRecord(Fnr.of("12345678901")).hendelse?.datoFrist)
            .isEqualTo(forventetDato.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.of("Europe/Oslo")))
    }

    @Test
    fun `beregnAvsluttesAutomatiskDato - tilFilterhendelseRecord - skal sette datoFrist null forlengelse opprettes`() {
        val hendelse = forlengelseHendelse(ForlengelseHendelseType.FORLENGELSE_OPPRETTET)

        assertThat(hendelse.tilFilterhendelseRecord(Fnr.of("12345678901")).hendelse?.datoFrist).isNull()
    }

    @Test
    fun `beregnAvsluttesAutomatiskDato - settes til 28 dager etter utlopstidspunkt når forlengelse utloper`() {
        val hendelseTidspunkt = ZonedDateTime.now().toInstant()
        val kandidatForUtmelding = forlengelseHendelse(
            ForlengelseHendelseType.FORLENGELSE_UTLOPT,
            hendelseTidspunkt = hendelseTidspunkt,
            forlengetTil = null,
        ).let { KandidatForUtmelding.fromHendelse(it) as AktivKandidatForUtmelding }

        val forventetDato = LocalDateTime.ofInstant(hendelseTidspunkt, ZoneOffset.UTC)
            .plusDays(KARENSTID_DAGER)

        val automatiskAvslutningDato = kandidatForUtmelding.avsluttesAutomatiskDato

        assertThat(automatiskAvslutningDato).isEqualTo(forventetDato)
        assertThat(kandidatForUtmelding.sisteHendelse.tilFilterhendelseRecord(Fnr.of("12345678901")).hendelse?.datoFrist)
            .isEqualTo(forventetDato.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.of("Europe/Oslo")))
    }
}
