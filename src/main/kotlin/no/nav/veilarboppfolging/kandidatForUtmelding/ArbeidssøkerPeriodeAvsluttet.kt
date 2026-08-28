package no.nav.veilarboppfolging.kandidatForUtmelding

import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NorskIdent
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.BeskrivelseEnum
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.FilterhendelseRecord
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Kategori
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Operasjon
import org.postgresql.util.PGobject
import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

class ArbeidssøkerPeriodeAvsluttet(
    oppfolgingsperiodeUuid: UUID,
    utfortAvType: KandidatForUtmeldingHendelseUtfortAvType,
    utfortAv: String?,
    kilde: String,
    arbeidssokerperiodeAvsluttetHendelseType: ArbeidssokerperiodeAvsluttetHendelseType,
    val avslutningsarsak: String?,
    hendelseTidspunkt: Instant,
) : KandidatForUtmeldingHendelse(
    oppfolgingsperiodeUuid,
    utfortAvType,
    utfortAv,
    kilde,
    hendelseTidspunkt,
) {
    override val type: ArbeidssokerperiodeAvsluttetHendelseType = arbeidssokerperiodeAvsluttetHendelseType
    val avsluttesAutomatiskDato: ZonedDateTime? = beregnAvsluttesAutomatiskDatoZonedDateTime()
    override val hendelseDataJson: PGobject? = avslutningsarsak?.let {
        PGobject().apply {
            type = "jsonb"
            value = JsonUtils.getMapper().writeValueAsString(Detaljer(it))
        }
    }

    data class Detaljer(
        val avslutningsarsak: String?
    )

    override fun tilFilterhendelseRecord(fnr: Fnr, operasjon: Operasjon): FilterhendelseRecord {
        return FilterhendelseRecord(
            personID = NorskIdent(fnr.get()),
            kategori = Kategori.KANDIDAT_FOR_UTMELDING,
            operasjon = operasjon,
            hendelse = FilterhendelseRecord.HendelseInnhold(
                beskrivelse = when (type) {
                    ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT -> "Arbeidssøkerperiode avsluttet: Ikke levert meldekort"
                    ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE -> "Arbeidssøkerperiode avsluttet: Svarte nei i bekreftelse"
                    ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET -> "Arbeidssøkerperiode avsluttet"
                },
                beskrivelseEnum = when (type) {
                    ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT -> BeskrivelseEnum.ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT
                    ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE -> BeskrivelseEnum.ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE
                    ArbeidssokerperiodeAvsluttetHendelseType.ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET -> BeskrivelseEnum.ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET
                }.name,
                dato = hendelseTidspunkt.atZone(ZoneId.of("Europe/Oslo")),
                lenke = URI("${baseUrlVeilarbpersonflate()}/aktivitetsplan").toURL(),
                detaljer = null,
                datoFrist = avsluttesAutomatiskDato
            )
        )
    }
}