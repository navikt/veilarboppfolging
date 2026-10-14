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

class AvsluttetAapSak(
    oppfolgingsperiodeUuid: UUID,
    val avslutningsarsak: String?,
    hendelseTidspunkt: Instant,
) : KandidatForUtmeldingHendelse(
    oppfolgingsperiodeUuid,
    KandidatForUtmeldingHendelseUtfortAvType.SYSTEM,
    "Kelvin",
    "Kelvin",
    hendelseTidspunkt,
) {
    val avsluttesAutomatiskDato: ZonedDateTime = beregnAvsluttesAutomatiskDato(hendelseTidspunkt)
    override val type: KandidatForUtmeldingHendelseType = IkkeLengerAapHendelseType.AVSLUTTET_AAP_SAK
    override val hendelseDataJson: PGobject? = null

    override fun tilFilterhendelseRecord(fnr: Fnr): FilterhendelseRecord {
        return FilterhendelseRecord(
            personID = NorskIdent(fnr.get()),
            kategori = Kategori.KANDIDAT_FOR_UTMELDING,
            operasjon = Operasjon.START,
            hendelse = FilterhendelseRecord.HendelseInnhold(
                beskrivelse = "Ikke lenger AAP",
                beskrivelseEnum = BeskrivelseEnum.AVSLUTTET_AAP_SAK.name,
                tidspunkt = hendelseTidspunkt.atZone(ZoneId.of("Europe/Oslo")),
                lenke = URI("${baseUrlVeilarbpersonflate()}/aktivitetsplan").toURL(),
                detaljer = null,
                tidspunktFrist = avsluttesAutomatiskDato
            )
        )
    }
}
