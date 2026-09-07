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
    val forlengetTil: LocalDate?
) : KandidatForUtmeldingHendelse(
    oppfolgingsperiodeUuid,
    utfortAvType,
    utfortAv,
    kilde,
    hendelseTidspunkt,
) {
    companion object {
        fun forlengelseUtløpt(oppfolgingsperiodeUuid: UUID): ForlengelseHendelse {
            return ForlengelseHendelse(
                oppfolgingsperiodeUuid = oppfolgingsperiodeUuid,
                utfortAvType = KandidatForUtmeldingHendelseUtfortAvType.SYSTEM,
                utfortAv = "SYSTEM",
                kilde = "veilarboppfolging",
                forlengelseHendelseType = ForlengelseHendelseType.FORLENGELSE_UTLOPT,
                hendelseTidspunkt = Instant.now(),
                forlengetTil = null
            )
        }
    }

    override val type: ForlengelseHendelseType = forlengelseHendelseType
    val avsluttesAutomatiskDato: ZonedDateTime = beregnAvsluttesAutomatiskDato(hendelseTidspunkt)

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

    override fun tilFilterhendelseRecord(fnr: Fnr): FilterhendelseRecord {
        return FilterhendelseRecord(
            personID = NorskIdent(fnr.get()),
            kategori = Kategori.KANDIDAT_FOR_UTMELDING,
            operasjon = when (type) {
                ForlengelseHendelseType.FORLENGELSE_OPPRETTET,
                ForlengelseHendelseType.FORLENGELSE_ENDRET -> Operasjon.STOPP
                ForlengelseHendelseType.FORLENGELSE_UTLOPT -> Operasjon.START
            },
            hendelse = FilterhendelseRecord.HendelseInnhold(
                beskrivelse = when (type) {
                    ForlengelseHendelseType.FORLENGELSE_UTLOPT -> "Forlengelse utløpt"
                    ForlengelseHendelseType.FORLENGELSE_OPPRETTET -> "Forlengelse opprettet"
                    ForlengelseHendelseType.FORLENGELSE_ENDRET -> "Forlengelse opprettet"
                },
                beskrivelseEnum = when (type) {
                    ForlengelseHendelseType.FORLENGELSE_UTLOPT -> BeskrivelseEnum.FORLENGELSE_UTLOPT
                    ForlengelseHendelseType.FORLENGELSE_OPPRETTET -> BeskrivelseEnum.FORLENGELSE_OPPRETTET
                    ForlengelseHendelseType.FORLENGELSE_ENDRET -> BeskrivelseEnum.FORLENGELSE_ENDRET
                }.name,
                dato = hendelseTidspunkt.atZone(ZoneId.of("Europe/Oslo")),
                lenke = URI("${baseUrlVeilarbpersonflate()}/aktivitetsplan").toURL(),
                detaljer = null,
                datoFrist = when(type) {
                    ForlengelseHendelseType.FORLENGELSE_UTLOPT -> avsluttesAutomatiskDato
                    ForlengelseHendelseType.FORLENGELSE_OPPRETTET,
                    ForlengelseHendelseType.FORLENGELSE_ENDRET -> null
                }
            )
        )
    }
}