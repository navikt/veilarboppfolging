package no.nav.veilarboppfolging.kandidatForUtmelding.hendelser

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
import no.nav.veilarboppfolging.kandidatForUtmelding.AktivKandidatForUtmelding
import no.nav.veilarboppfolging.kandidatForUtmelding.ForlengetKandidat
import no.nav.veilarboppfolging.kandidatForUtmelding.KandidatForUtmelding
import java.time.LocalDate
import no.nav.veilarboppfolging.kandidatForUtmelding.beregnAvsluttesAutomatiskDato

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
                tidspunkt = hendelseTidspunkt.atZone(ZoneId.of("Europe/Oslo")),
                lenke = URI("${baseUrlVeilarbpersonflate()}/aktivitetsplan").toURL(),
                detaljer = null,
                tidspunktFrist = avsluttesAutomatiskDato
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
    override val type: KandidatForUtmeldingHendelseType
): KandidatForUtmeldingHendelse(
    oppfolgingsperiodeUuid,
    utfortAvType,
    utfortAv,
    kilde,
    hendelseTidspunkt,
) {

    constructor(
        kandidat: KandidatForUtmelding,
        utfortAvType: KandidatForUtmeldingHendelseUtfortAvType,
        utfortAv: String?,
        kilde: String,
        hendelseTidspunkt: Instant,
        forlengetTil: LocalDate) :
    this(
        oppfolgingsperiodeUuid = kandidat.oppfolgingsperiodeId,
        hendelseTidspunkt = hendelseTidspunkt,
        utfortAvType = utfortAvType,
        utfortAv = utfortAv,
        kilde = kilde,
        forlengetTil = forlengetTil,
        type = when (kandidat) {
            is AktivKandidatForUtmelding -> ForlengelseHendelseType.FORLENGELSE_OPPRETTET
            is ForlengetKandidat -> ForlengelseHendelseType.FORLENGELSE_ENDRET
        }
    )


    data class Detaljer(
        val forlengetTil: LocalDate,
        val forrigeHendelseType: KandidatForUtmeldingHendelseType?,
    )

    override val hendelseDataJson: PGobject = PGobject().apply {
        type = "jsonb"
        value = JsonUtils.getMapper().writeValueAsString(Detaljer(forlengetTil, forrigeHendelseType))
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
