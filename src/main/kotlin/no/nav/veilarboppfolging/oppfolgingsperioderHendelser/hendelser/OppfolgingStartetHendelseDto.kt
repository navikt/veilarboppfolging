package no.nav.veilarboppfolging.oppfolgingsperioderHendelser.hendelser

import no.nav.veilarboppfolging.oppfolgingsperioderHendelser.OppfolgingsPeriodeHendelseDto
import java.time.ZonedDateTime
import java.util.*

class OppfolgingStartetHendelseDto(
    val oppfolgingsPeriodeId: UUID,
    val startetTidspunkt: ZonedDateTime,
    val startetAv: String,
    val startetAvType: String,
    val startetBegrunnelse: String,
    val arenaKontor: String?,
    val arbeidsoppfolgingsKontorSattAvVeileder: String?,
    val fnr: String,
): OppfolgingsPeriodeHendelseDto(HendelseType.OPPFOLGING_STARTET)
