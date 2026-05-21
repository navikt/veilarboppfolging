package no.nav.veilarboppfolging.domain

import java.time.LocalDate

class AvslutningsStatusData(
    var kanAvslutte: Boolean,
    var underOppfolging: Boolean,
    var harYtelser: Boolean,
    var underKvp: Boolean,
    var inaktiveringsDato: LocalDate?,
    var erIserv: Boolean,
    var harAktiveTiltaksdeltakelser: Boolean,
    var erDeltakerIUngdomsprogrammet: Boolean,
    var erArbeidssoeker: Boolean,
)
