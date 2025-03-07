package no.nav.veilarboppfolging.oppfolgingsbruker.inngang

import no.nav.veilarboppfolging.client.pdl.ForenkletFolkeregisterStatus
import no.nav.veilarboppfolging.controller.TilgangResultat

sealed class KanStarteOppfolgingSjekk {
    infix fun and(kanStarteOppfolging: Lazy<KanStarteOppfolgingSjekk>): KanStarteOppfolging {
        val isOk = when (this) {
            is BrukerHarRiktigOppfolgingStatus -> (this is OPPFOLGING_OK)
            is VeilederHarTilgang -> (this is TILGANG_OK)
            is BrukerHarRiktigFregStatus -> (this is FREG_STATUS_OK)
        }
        if (!isOk) return KanStarteOppfolging.kanStarteOppfolging(this)
        return KanStarteOppfolging.kanStarteOppfolging(kanStarteOppfolging.value)
    }
}

sealed class BrukerHarRiktigOppfolgingStatus(): KanStarteOppfolgingSjekk()
object OPPFOLGING_OK: BrukerHarRiktigOppfolgingStatus()
object ALLEREDE_UNDER_OPPFOLGING: BrukerHarRiktigOppfolgingStatus()

sealed class VeilederHarTilgang: KanStarteOppfolgingSjekk()
object TILGANG_OK: VeilederHarTilgang()
object IKKE_TILGANG_FORTROLIG_ADRESSE: VeilederHarTilgang()
object IKKE_TILGANG_STRENGT_FORTROLIG_ADRESSE: VeilederHarTilgang()
object IKKE_TILGANG_EGNE_ANSATTE: VeilederHarTilgang()
object IKKE_TILGANG_ENHET: VeilederHarTilgang()
object IKKE_TILGANG_MODIA: VeilederHarTilgang()

sealed class BrukerHarRiktigFregStatus: KanStarteOppfolgingSjekk()
object FREG_STATUS_OK: BrukerHarRiktigFregStatus()
object DOD: BrukerHarRiktigFregStatus()
object IKKE_LOVLIG_OPPHOLD: BrukerHarRiktigFregStatus()
object UKJENT_STATUS_FOLKEREGISTERET: BrukerHarRiktigFregStatus()
object INGEN_STATUS_FOLKEREGISTERET: BrukerHarRiktigFregStatus()

enum class KanStarteOppfolging {
    JA,
    ALLEREDE_UNDER_OPPFOLGING,
    DOD,
    IKKE_LOVLIG_OPPHOLD,
    UKJENT_STATUS_FOLKEREGISTERET,
    INGEN_STATUS_FOLKEREGISTERET,
    IKKE_TILGANG_FORTROLIG_ADRESSE,
    IKKE_TILGANG_STRENGT_FORTROLIG_ADRESSE,
    IKKE_TILGANG_EGNE_ANSATTE,
    IKKE_TILGANG_ENHET,
    IKKE_TILGANG_MODIA;

    infix fun and(kanStarteOppfolging: Lazy<KanStarteOppfolgingSjekk>): KanStarteOppfolging {
        if (this != KanStarteOppfolging.JA) return this
        return kanStarteOppfolging(kanStarteOppfolging.value)
    }

    companion object {
        fun kanStarteOppfolging(kanStarteOppfolgingSjekk: KanStarteOppfolgingSjekk): KanStarteOppfolging {
            return when (kanStarteOppfolgingSjekk) {
                is DOD -> DOD
                is FREG_STATUS_OK -> JA
                is OPPFOLGING_OK -> JA
                is TILGANG_OK -> JA
                is IKKE_LOVLIG_OPPHOLD -> IKKE_LOVLIG_OPPHOLD
                is UKJENT_STATUS_FOLKEREGISTERET -> UKJENT_STATUS_FOLKEREGISTERET
                is ALLEREDE_UNDER_OPPFOLGING -> ALLEREDE_UNDER_OPPFOLGING
                is IKKE_TILGANG_EGNE_ANSATTE -> IKKE_TILGANG_EGNE_ANSATTE
                is IKKE_TILGANG_ENHET -> IKKE_TILGANG_ENHET
                is IKKE_TILGANG_MODIA -> IKKE_TILGANG_MODIA
                is IKKE_TILGANG_FORTROLIG_ADRESSE -> IKKE_TILGANG_FORTROLIG_ADRESSE
                is IKKE_TILGANG_STRENGT_FORTROLIG_ADRESSE -> IKKE_TILGANG_STRENGT_FORTROLIG_ADRESSE
                is INGEN_STATUS_FOLKEREGISTERET -> INGEN_STATUS_FOLKEREGISTERET
            }
        }
    }
}

fun TilgangResultat.toKanStarteOppfolging(): VeilederHarTilgang {
    return when (this) {
        TilgangResultat.HAR_TILGANG -> TILGANG_OK
        TilgangResultat.IKKE_TILGANG_FORTROLIG_ADRESSE -> IKKE_TILGANG_FORTROLIG_ADRESSE
        TilgangResultat.IKKE_TILGANG_STRENGT_FORTROLIG_ADRESSE -> IKKE_TILGANG_STRENGT_FORTROLIG_ADRESSE
        TilgangResultat.IKKE_TILGANG_EGNE_ANSATTE -> IKKE_TILGANG_EGNE_ANSATTE
        TilgangResultat.IKKE_TILGANG_ENHET -> IKKE_TILGANG_ENHET
        TilgangResultat.IKKE_TILGANG_MODIA -> IKKE_TILGANG_MODIA
    }
}

fun ForenkletFolkeregisterStatus.toKanStarteOppfolging(): BrukerHarRiktigFregStatus {
    return when (this) {
        ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven,
        ForenkletFolkeregisterStatus.dNummer -> FREG_STATUS_OK
        ForenkletFolkeregisterStatus.opphoert,
        ForenkletFolkeregisterStatus.ikkeBosatt,
        ForenkletFolkeregisterStatus.forsvunnet-> IKKE_LOVLIG_OPPHOLD
        ForenkletFolkeregisterStatus.doedIFolkeregisteret -> DOD
        ForenkletFolkeregisterStatus.ukjent -> UKJENT_STATUS_FOLKEREGISTERET
        ForenkletFolkeregisterStatus.ingen_status -> INGEN_STATUS_FOLKEREGISTERET
    }
}
