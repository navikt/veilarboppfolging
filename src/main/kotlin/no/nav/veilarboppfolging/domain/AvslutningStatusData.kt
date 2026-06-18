package no.nav.veilarboppfolging.domain

import java.time.LocalDate

data class AvslutningStatusData(
    val kanAvslutte: Boolean,
    val underOppfolging: Boolean,
    val harYtelser: Boolean,
    val underKvp: Boolean,
    val inaktiveringsDato: LocalDate?,
    val erIserv: Boolean,
    val harAktiveTiltaksdeltakelser: Boolean,
    val erDeltakerIUngdomsprogrammet: Boolean,
    val erArbeidssoeker: Boolean,
    val harAap: Boolean,
)