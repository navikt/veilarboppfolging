package no.nav.veilarboppfolging.domain

import java.time.LocalDate
import no.nav.veilarboppfolging.oppfolgingsbruker.utgang.AvslutningsBegrunnelse

data class AvslutningStatusData(
    val kanAvslutte: Boolean,
    val underOppfolging: Boolean,
    val underKvp: Boolean,
    val inaktiveringsDato: LocalDate?,
    val erIserv: Boolean,
    val harAktiveTiltaksdeltakelser: Boolean,
    val erDeltakerIUngdomsprogrammet: Boolean,
    val erArbeidssoeker: Boolean,
    val harAap: Boolean,
    val begrunnelse: AvslutningsBegrunnelse?
)