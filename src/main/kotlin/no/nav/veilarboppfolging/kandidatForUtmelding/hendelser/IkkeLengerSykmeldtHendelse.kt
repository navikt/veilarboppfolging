package no.nav.veilarboppfolging.kandidatForUtmelding.hendelser

import java.net.URI
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import no.nav.common.json.JsonUtils
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NorskIdent
import no.nav.veilarboppfolging.kandidatForUtmelding.beregnAvsluttesAutomatiskDato
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.BeskrivelseEnum
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.FilterhendelseRecord
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Kategori
import no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse.Operasjon
import org.postgresql.util.PGobject

class IkkeLengerSykmeldtHendelse(
    oppfolgingsperiodeUuid: UUID,
    val syketilfelleAvsluttetDato: LocalDate,
    hendelseTidspunkt: Instant,
) : KandidatForUtmeldingHendelse(
    oppfolgingsperiodeUuid,
    KandidatForUtmeldingHendelseUtfortAvType.SYSTEM,
    "Modia sykefravær",
    "Modia sykefravær",
    hendelseTidspunkt,
) {
    override val type: IkkeLengerSykmeldtHendelseType = IkkeLengerSykmeldtHendelseType.IKKE_LENGER_SYKMELDT

    val avsluttesAutomatiskDato: ZonedDateTime = beregnAvsluttesAutomatiskDato(hendelseTidspunkt)

    override val hendelseDataJson: PGobject = PGobject().apply {
        type = "jsonb"
        value = JsonUtils.getMapper().writeValueAsString(Detaljer(syketilfelleAvsluttetDato))
    }

    data class Detaljer(
        val syketilfelleAvsluttetDato: LocalDate,
    )

    override fun tilFilterhendelseRecord(fnr: Fnr): FilterhendelseRecord {
        return FilterhendelseRecord(
            personID = NorskIdent(fnr.get()),
            kategori = Kategori.KANDIDAT_FOR_UTMELDING,
            operasjon = Operasjon.START,
            hendelse = FilterhendelseRecord.HendelseInnhold(
                beskrivelse = "Ikke lenger sykmeldt",
                beskrivelseEnum = BeskrivelseEnum.IKKE_LENGER_SYKMELDT.name,
                tidspunkt = hendelseTidspunkt.atZone(ZoneId.of("Europe/Oslo")),
                lenke = URI("${baseUrlVeilarbpersonflate()}/aktivitetsplan").toURL(),
                detaljer = null,
                tidspunktFrist = avsluttesAutomatiskDato
            )
        )
    }
}