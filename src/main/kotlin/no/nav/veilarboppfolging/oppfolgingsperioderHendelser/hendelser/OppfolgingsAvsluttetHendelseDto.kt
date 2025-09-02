package no.nav.veilarboppfolging.oppfolgingsperioderHendelser.hendelser

import no.nav.veilarboppfolging.oppfolgingsperioderHendelser.OppfolgingsPeriodeHendelseDto
import java.time.ZonedDateTime
import java.util.UUID

class OppfolgingsAvsluttetHendelseDto(
    val oppfolgingsPeriodeId: UUID,
    val startetTidspunkt: ZonedDateTime,
    val avsluttetTidspunkt: ZonedDateTime,
    val avsluttetAv: String,
    val avsluttetAvType: String,
    val avsluttetBegrunnelse: String,
): OppfolgingsPeriodeHendelseDto(HendelseType.OPPFOLGING_STARTET)
