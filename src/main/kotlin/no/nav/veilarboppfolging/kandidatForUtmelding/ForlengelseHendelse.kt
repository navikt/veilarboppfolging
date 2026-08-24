package no.nav.veilarboppfolging.kandidatForUtmelding

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
import java.util.UUID

class ForlengelseHendelse(
    oppfolgingsperiodeUuid: UUID,
    utfortAvType: KandidatForUtmeldingHendelseUtfortAvType,
    utfortAv: String?,
    kilde: String,
    val forlengelseHendelseType: KandidatForUtmeldingHendelseType,
    hendelseTidspunkt: Instant,
    forlengetTilTidspunkt: Instant,
) : KandidatForUtmeldingHendelse(
    oppfolgingsperiodeUuid,
    utfortAvType,
    utfortAv,
    kilde,
    hendelseTidspunkt,
) {
    override val type: KandidatForUtmeldingHendelseType = forlengelseHendelseType
    override val hendelseDataJson: PGobject? = null

    override fun tilFilterhendelseRecord(fnr: Fnr, operasjon: Operasjon): FilterhendelseRecord {
        return FilterhendelseRecord(
            personID = NorskIdent(fnr.get()),
            kategori = Kategori.KANDIDAT_FOR_UTMELDING,
            operasjon = operasjon,
            hendelse = FilterhendelseRecord.HendelseInnhold(
                beskrivelse = when (type) {
                    KandidatForUtmeldingHendelseType.FORLENGELSE_UTLOPT -> "Forlengelse utløpt"
                    else -> throw IllegalArgumentException("Ugyldig forlengelseshendelsestype for filterhendelser")
                },
                beskrivelseEnum = when (type) {
                    KandidatForUtmeldingHendelseType.FORLENGELSE_UTLOPT -> BeskrivelseEnum.FORLENGELSE_UTLOPT
                    else -> throw IllegalArgumentException("Ugyldig forlengelseshendelsestype for filterhendelser")
                }.name,
                dato = hendelseTidspunkt.atZone(ZoneId.of("Europe/Oslo")),
                lenke = URI("${baseUrlVeilarbpersonflate()}/aktivitetsplan").toURL(),
                detaljer = null,
            )
        )
    }
}