package no.nav.veilarboppfolging.controller.response

import java.time.LocalDate

data class AvslutningsStatusDto(
    val kanAvslutte: Boolean,
    val underOppfolging: Boolean,
    val harYtelser: Boolean,
    val underKvp: Boolean,
    val inaktiveringsDato: LocalDate?,
    val erIserv: Boolean,
    val harAktiveTiltaksdeltakelser: Boolean,
    val erDeltakerIUngdomsprogrammet: Boolean,
    val erArbeidssoeker: Boolean,
)