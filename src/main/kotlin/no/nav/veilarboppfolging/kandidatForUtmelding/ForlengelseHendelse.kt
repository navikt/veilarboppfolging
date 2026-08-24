package no.nav.veilarboppfolging.kandidatForUtmelding

import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NorskIdent
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.BeskrivelseEnum
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.FilterhendelseRecord
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Kategori
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Operasjon
import org.postgresql.util.PGobject
import java.time.ZonedDateTime
import no.nav.common.json.JsonUtils
import java.time.LocalDate

class ForlengelseHendelse(
    oppfolgingsperiodeUuid: UUID,
    utfortAvType: KandidatForUtmeldingHendelseUtfortAvType,
    utfortAv: String?,
    kilde: String,
    val forlengelseHendelseType: ForlengelseHendelseType,
    hendelseTidspunkt: Instant,
    val forlengetTil: LocalDate?,
) : KandidatForUtmeldingHendelse(
    oppfolgingsperiodeUuid,
    utfortAvType,
    utfortAv,
    kilde,
    hendelseTidspunkt,
) {
    override val type: KandidatForUtmeldingHendelseType = forlengelseHendelseType
    override val hendelseDataJson: PGobject? = forlengetTil?.let {
        PGobject().apply {
            type = "jsonb"
            value = JsonUtils.getMapper().writeValueAsString(Detaljer(it))
        }
    }

    data class Detaljer(
        val forlengetTil: LocalDate?,
    )

    fun hentForlengetTil(): LocalDate? {
        return when (forlengelseHendelseType) {
            ForlengelseHendelseType.FORLENGELSE_OPPRETTET,
            ForlengelseHendelseType.FORLENGELSE_ENDRET -> forlengetTil
            ForlengelseHendelseType.FORLENGELSE_UTLOPT -> null
        }
    }

    override fun tilFilterhendelseRecord(fnr: Fnr, operasjon: Operasjon): FilterhendelseRecord {
        return FilterhendelseRecord(
            personID = NorskIdent(fnr.get()),
            kategori = Kategori.KANDIDAT_FOR_UTMELDING,
            operasjon = operasjon,
            hendelse = FilterhendelseRecord.HendelseInnhold(
                beskrivelse = when (type) {
                    ForlengelseHendelseType.FORLENGELSE_UTLOPT -> "Forlengelse utløpt"
                    else -> throw IllegalArgumentException("Ugyldig forlengelseshendelsestype for filterhendelser")
                },
                beskrivelseEnum = when (type) {
                    ForlengelseHendelseType.FORLENGELSE_UTLOPT -> BeskrivelseEnum.FORLENGELSE_UTLOPT
                    else -> throw IllegalArgumentException("Ugyldig forlengelseshendelsestype for filterhendelser")
                }.name,
                dato = hendelseTidspunkt.atZone(ZoneId.of("Europe/Oslo")),
                lenke = URI("${baseUrlVeilarbpersonflate()}/aktivitetsplan").toURL(),
                detaljer = null,
            )
        )
    }
}