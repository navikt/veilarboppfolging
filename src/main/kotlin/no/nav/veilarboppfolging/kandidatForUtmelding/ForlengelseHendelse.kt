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

class ForlengelseUtløptHendelse(
    oppfolgingsperiodeUuid: UUID,
    hendelseTidspunkt: Instant,
): KandidatForUtmeldingHendelse(
    oppfolgingsperiodeUuid,
    KandidatForUtmeldingHendelseUtfortAvType.SYSTEM,
    "veilarboppfolging",
    "veilarboppfolging",
    hendelseTidspunkt,
) {
    val avsluttesAutomatiskDato: ZonedDateTime = beregnAvsluttesAutomatiskDato(hendelseTidspunkt)
    override val type: KandidatForUtmeldingHendelseType = ForlengelseHendelseType.FORLENGELSE_UTLOPT
    override val hendelseDataJson: PGobject? = null
    override fun tilFilterhendelseRecord(fnr: Fnr): FilterhendelseRecord {
        return FilterhendelseRecord(
            personID = NorskIdent(fnr.get()),
            kategori = Kategori.KANDIDAT_FOR_UTMELDING,
            operasjon = Operasjon.START,
            hendelse = FilterhendelseRecord.HendelseInnhold(
                beskrivelse = "Forlengelse utløpt",
                beskrivelseEnum = BeskrivelseEnum.FORLENGELSE_UTLOPT.name,
                dato = hendelseTidspunkt.atZone(ZoneId.of("Europe/Oslo")),
                lenke = URI("${baseUrlVeilarbpersonflate()}/aktivitetsplan").toURL(),
                detaljer = null,
                datoFrist = avsluttesAutomatiskDato
            )
        )
    }
}

class ForlengelseOpprettetEllerEndretHendelse(
    oppfolgingsperiodeUuid: UUID,
    hendelseTidspunkt: Instant,
    utfortAvType: KandidatForUtmeldingHendelseUtfortAvType,
    utfortAv: String?,
    kilde: String,
    val forlengetTil: LocalDate,
    forlengelseHendelseType: ForlengelseHendelseType
): KandidatForUtmeldingHendelse(
    oppfolgingsperiodeUuid,
    utfortAvType,
    utfortAv,
    kilde,
    hendelseTidspunkt,
) {
    override val type: KandidatForUtmeldingHendelseType = forlengelseHendelseType
    data class Detaljer(
        val forlengetTil: LocalDate,
    )
    override val hendelseDataJson: PGobject = forlengetTil.let {
        PGobject().apply {
            type = "jsonb"
            value = JsonUtils.getMapper().writeValueAsString(Detaljer(it))
        }
    }
    override fun tilFilterhendelseRecord(fnr: Fnr): FilterhendelseRecord {
        return FilterhendelseRecord(
            personID = NorskIdent(fnr.get()),
            kategori = Kategori.KANDIDAT_FOR_UTMELDING,
            operasjon = Operasjon.STOPP,
            hendelse = null
        )
    }
}
