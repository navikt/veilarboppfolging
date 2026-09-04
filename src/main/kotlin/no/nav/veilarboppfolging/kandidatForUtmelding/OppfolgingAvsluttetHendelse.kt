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
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

class OppfolgingAvsluttetHendelse(
    oppfolgingsperiodeUuid: UUID,
    utfortAvType: KandidatForUtmeldingHendelseUtfortAvType,
    utfortAv: String?,
    kilde: String,
    hendelseTidspunkt: Instant,
    val oppfolgingAvsluttetHendelseType: OppfolgingAvsluttetHendelseType,
) :
    KandidatForUtmeldingHendelse(
        oppfolgingsperiodeUuid,
        utfortAvType,
        utfortAv,
        kilde,
        hendelseTidspunkt
    ) {
    override val type:  OppfolgingAvsluttetHendelseType = oppfolgingAvsluttetHendelseType
    override val hendelseDataJson: PGobject? = null
    override fun tilFilterhendelseRecord(fnr: Fnr, operasjon: Operasjon): FilterhendelseRecord {
        return FilterhendelseRecord(
            personID = NorskIdent(fnr.get()),
            kategori = Kategori.KANDIDAT_FOR_UTMELDING,
            operasjon = operasjon,
            hendelse = FilterhendelseRecord.HendelseInnhold(
                beskrivelse = when (type) {
                    OppfolgingAvsluttetHendelseType.OPPFOLGING_AVSLUTTET_AUTOMATISK -> "Oppfølging avsluttet automatisk"
                    OppfolgingAvsluttetHendelseType.OPPFOLGING_AVSLUTTET_MANUELT -> "Oppfølging avsluttet manuelt"
                    else -> throw IllegalArgumentException("Ugyldig forlengelseshendelsestype for filterhendelser")
                },
                beskrivelseEnum = when (type) {
                    OppfolgingAvsluttetHendelseType.OPPFOLGING_AVSLUTTET_AUTOMATISK -> BeskrivelseEnum.OPPFOLGING_AVSLUTTET_AUTOMATISK
                    OppfolgingAvsluttetHendelseType.OPPFOLGING_AVSLUTTET_MANUELT -> BeskrivelseEnum.OPPFOLGING_AVSLUTTET_MANUELT
                    else -> throw IllegalArgumentException("Ugyldig forlengelseshendelsestype for filterhendelser")
                }.name,
                dato = hendelseTidspunkt.atZone(ZoneId.of("Europe/Oslo")),
                lenke = URI("${baseUrlVeilarbpersonflate()}/aktivitetsplan").toURL(),
                detaljer = null,
                datoFrist = null
            )
        )
    }
}

enum class OppfolgingAvsluttetHendelseType : KandidatForUtmeldingHendelseType {
    OPPFOLGING_AVSLUTTET_AUTOMATISK,
    OPPFOLGING_AVSLUTTET_MANUELT
}
