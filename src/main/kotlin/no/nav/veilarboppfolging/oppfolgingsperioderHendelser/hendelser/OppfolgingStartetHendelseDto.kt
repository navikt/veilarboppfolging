package no.nav.veilarboppfolging.oppfolgingsperioderHendelser.hendelser

import no.nav.veilarboppfolging.oppfolgingsbruker.StartetAvType
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingStartBegrunnelse
import no.nav.veilarboppfolging.oppfolgingsperioderHendelser.OppfolgingsHendelseDto
import java.time.ZonedDateTime
import java.util.*

class OppfolgingStartetHendelseDto(
    val oppfolgingsPeriodeId: UUID,
    val startetTidspunkt: ZonedDateTime,
    val startetAv: String,
    val startetAvType: StartetAvType,
    val startetBegrunnelse: OppfolgingStartBegrunnelse,
    val arenaKontor: String?,
    val arenaKontorSistEndret: ZonedDateTime?,
    val foretrukketArbeidsoppfolgingskontor: String?,
    val fnr: String,
): OppfolgingsHendelseDto(HendelseType.OPPFOLGING_STARTET)
