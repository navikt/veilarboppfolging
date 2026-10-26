package no.nav.veilarboppfolging.kandidatForUtmelding.hendelser

import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NorskIdent
import no.nav.veilarboppfolging.kandidatForUtmelding.beregnAvsluttesAutomatiskDato
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

class EndretFolkeregisterstatus(
    oppfolgingsperiodeUuid: UUID,
    hendelseTidspunkt: Instant,
    endretFolkeregisterstatusHendelseType: EndretFolkeregisterstatusHendelseType,
) : KandidatForUtmeldingHendelse(
    oppfolgingsperiodeUuid,
    KandidatForUtmeldingHendelseUtfortAvType.SYSTEM,
    "Folkeregisteret",
    "PDL",
    hendelseTidspunkt,
) {
    val avsluttesAutomatiskDato: ZonedDateTime = beregnAvsluttesAutomatiskDato(hendelseTidspunkt)
    override val type: EndretFolkeregisterstatusHendelseType = endretFolkeregisterstatusHendelseType
    override val hendelseDataJson: PGobject? = null

    override fun tilFilterhendelseRecord(fnr: Fnr): FilterhendelseRecord {
        return FilterhendelseRecord(
            personID = NorskIdent(fnr.get()),
            kategori = Kategori.KANDIDAT_FOR_UTMELDING,
            operasjon = Operasjon.START,
            hendelse = FilterhendelseRecord.HendelseInnhold(
                beskrivelse = when (type) {
                    EndretFolkeregisterstatusHendelseType.FOLKEREGISTERSTATUS_FORSVUNNET -> "Folkeregisterstatus: Forsvunnet"
                    EndretFolkeregisterstatusHendelseType.FOLKEREGISTERSTATUS_OPPHORT -> "Folkeregisterstatus: Opphørt"
                    EndretFolkeregisterstatusHendelseType.FOLKEREGISTERSTATUS_UKJENT -> "Folkeregisterstatus: Ukjent"
                    EndretFolkeregisterstatusHendelseType.FOLKEREGISTERSTATUS_INGEN_STATUS -> "Folkeregisterstatus: Ingen status"
                },
                beskrivelseEnum = when (type) {
                    EndretFolkeregisterstatusHendelseType.FOLKEREGISTERSTATUS_FORSVUNNET -> BeskrivelseEnum.FOLKEREGISTERSTATUS_FORSVUNNET
                    EndretFolkeregisterstatusHendelseType.FOLKEREGISTERSTATUS_OPPHORT -> BeskrivelseEnum.FOLKEREGISTERSTATUS_OPPHORT
                    EndretFolkeregisterstatusHendelseType.FOLKEREGISTERSTATUS_UKJENT -> BeskrivelseEnum.FOLKEREGISTERSTATUS_UKJENT
                    EndretFolkeregisterstatusHendelseType.FOLKEREGISTERSTATUS_INGEN_STATUS -> BeskrivelseEnum.FOLKEREGISTERSTATUS_INGEN_STATUS
                }.name,
                tidspunkt = hendelseTidspunkt.atZone(ZoneId.of("Europe/Oslo")),
                lenke = URI("${baseUrlVeilarbpersonflate()}/aktivitetsplan").toURL(),
                detaljer = null,
                tidspunktFrist = avsluttesAutomatiskDato
            )
        )
    }
}
