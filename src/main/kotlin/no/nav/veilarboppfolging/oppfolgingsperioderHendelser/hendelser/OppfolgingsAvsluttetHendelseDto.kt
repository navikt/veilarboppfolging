package no.nav.veilarboppfolging.oppfolgingsperioderHendelser.hendelser

import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.oppfolgingsbruker.AvsluttetAvType
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.Avregistrering
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.AvregistreringsType
import no.nav.veilarboppfolging.oppfolgingsperioderHendelser.OppfolgingsHendelseDto
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity
import java.time.ZonedDateTime
import java.util.UUID

class OppfolgingsAvsluttetHendelseDto(
    val fnr: String,
    val oppfolgingsPeriodeId: UUID,
    val startetTidspunkt: ZonedDateTime,
    val avsluttetTidspunkt: ZonedDateTime,
    val avsluttetAv: String,
    val avsluttetAvType: AvsluttetAvType,
    val avregistreringsType: AvregistreringsType
): OppfolgingsHendelseDto(HendelseType.OPPFOLGING_AVSLUTTET) {
    companion object {
        fun of(avregistrering: Avregistrering, periode: OppfolgingsperiodeEntity, fnr: Fnr): OppfolgingsAvsluttetHendelseDto {
            return OppfolgingsAvsluttetHendelseDto(
                fnr = fnr.get(),
                oppfolgingsPeriodeId = periode.uuid,
                startetTidspunkt = periode.startDato,
                avsluttetTidspunkt = periode.sluttDato,
                avsluttetAv = avregistrering.avsluttetAv.getIdent(),
                avsluttetAvType = avregistrering.avsluttetAv.getType(),
                avregistreringsType = avregistrering.getAvregistreringsType(),
            )
        }
    }
}
