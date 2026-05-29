package no.nav.veilarboppfolging.oppfolgingsbruker.arena

import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Hovedmaal
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import java.time.LocalDate

class LocalArenaOppfolging(
    /**
     * Hovedmaal er null før man får 14a vedtak
     */
    val hovedmaal: Hovedmaal?,
    val kvalifiseringsgruppe: Kvalifiseringsgruppe,
    val formidlingsgruppe: Formidlingsgruppe,
    /**
     * Null frem til man blir satt i ISERV
     */
    val iservFraDato: LocalDate?
)
