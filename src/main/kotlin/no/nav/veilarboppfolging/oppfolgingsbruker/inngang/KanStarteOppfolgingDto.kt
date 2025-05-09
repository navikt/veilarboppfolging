package no.nav.veilarboppfolging.oppfolgingsbruker.inngang

import no.nav.veilarboppfolging.client.pdl.ForenkletFolkeregisterStatus
import no.nav.veilarboppfolging.client.pdl.FregStatusOgStatsborgerskap
import no.nav.veilarboppfolging.client.pdl.eeaLand
import no.nav.veilarboppfolging.controller.TilgangResultat

sealed class KanStarteOppfolgingSjekk {
    fun oppfolgingSjekk(brukerHarRiktigOppfolgingStatus: Lazy<BrukerHarRiktigOppfolgingStatus>,
                        veilederHarTilgang: Lazy<VeilederHarTilgang>,
                        fregStatusSjekkResultat: Lazy<FregStatusSjekkResultat>): KanStarteOppfolgingDto {

        when(veilederHarTilgang.value){
            IKKE_TILGANG_EGNE_ANSATTE,
            IKKE_TILGANG_ENHET,
            IKKE_TILGANG_FORTROLIG_ADRESSE,
            IKKE_TILGANG_MODIA,
            IKKE_TILGANG_STRENGT_FORTROLIG_ADRESSE -> return KanStarteOppfolgingDto.kanStarteOppfolging(veilederHarTilgang.value)
            TILGANG_OK -> {}
        }

        when(fregStatusSjekkResultat.value){
            DOD,
            INGEN_STATUS_FOLKEREGISTERET,
            UKJENT_STATUS_FOLKEREGISTERET,
            IKKE_LOVLIG_OPPHOLD -> return KanStarteOppfolgingDto.kanStarteOppfolging(fregStatusSjekkResultat.value)
            FREG_STATUS_OK,
            FREG_STATUS_KREVER_MANUELL_GODKJENNING -> {}
        }

        return when(brukerHarRiktigOppfolgingStatus.value){
            ALLEREDE_UNDER_OPPFOLGING -> KanStarteOppfolgingDto.kanStarteOppfolging(brukerHarRiktigOppfolgingStatus.value)
            ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT -> {
                if(fregStatusSjekkResultat.value is FREG_STATUS_KREVER_MANUELL_GODKJENNING){
                    KanStarteOppfolgingDto.ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT_MEN_KREVER_MANUELL_GODKJENNING
                } else
                    KanStarteOppfolgingDto.ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT
            }
            OPPFOLGING_OK -> {
               if(fregStatusSjekkResultat.value is FREG_STATUS_KREVER_MANUELL_GODKJENNING) {
                    KanStarteOppfolgingDto.JA_MED_MANUELL_GODKJENNING
                }
                else {
                    KanStarteOppfolgingDto.JA
                }
            }
        }
    }
}

sealed class BrukerHarRiktigOppfolgingStatus(): KanStarteOppfolgingSjekk()
object OPPFOLGING_OK: BrukerHarRiktigOppfolgingStatus()
object ALLEREDE_UNDER_OPPFOLGING: BrukerHarRiktigOppfolgingStatus()
object ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT: BrukerHarRiktigOppfolgingStatus()

sealed class VeilederHarTilgang: KanStarteOppfolgingSjekk()
object TILGANG_OK: VeilederHarTilgang()
object IKKE_TILGANG_FORTROLIG_ADRESSE: VeilederHarTilgang()
object IKKE_TILGANG_STRENGT_FORTROLIG_ADRESSE: VeilederHarTilgang()
object IKKE_TILGANG_EGNE_ANSATTE: VeilederHarTilgang()
object IKKE_TILGANG_ENHET: VeilederHarTilgang()
object IKKE_TILGANG_MODIA: VeilederHarTilgang()

sealed class FregStatusSjekkResultat: KanStarteOppfolgingSjekk()
object FREG_STATUS_OK: FregStatusSjekkResultat()
object FREG_STATUS_KREVER_MANUELL_GODKJENNING: FregStatusSjekkResultat()
object DOD: FregStatusSjekkResultat()
object IKKE_LOVLIG_OPPHOLD: FregStatusSjekkResultat()
object UKJENT_STATUS_FOLKEREGISTERET: FregStatusSjekkResultat()
object INGEN_STATUS_FOLKEREGISTERET: FregStatusSjekkResultat()

enum class KanStarteOppfolgingDto {
    JA,
    JA_MED_MANUELL_GODKJENNING,
    ALLEREDE_UNDER_OPPFOLGING,
    ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT,
    ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT_MEN_KREVER_MANUELL_GODKJENNING,
    DOD,
    IKKE_LOVLIG_OPPHOLD,
    UKJENT_STATUS_FOLKEREGISTERET,
    INGEN_STATUS_FOLKEREGISTERET,
    IKKE_TILGANG_FORTROLIG_ADRESSE,
    IKKE_TILGANG_STRENGT_FORTROLIG_ADRESSE,
    IKKE_TILGANG_EGNE_ANSATTE,
    IKKE_TILGANG_ENHET,
    IKKE_TILGANG_MODIA;

    companion object {
        fun kanStarteOppfolging(kanStarteOppfolgingSjekk: KanStarteOppfolgingSjekk): KanStarteOppfolgingDto {
            return when (kanStarteOppfolgingSjekk) {
                is DOD -> DOD
                is FREG_STATUS_OK -> JA
                is FREG_STATUS_KREVER_MANUELL_GODKJENNING -> JA_MED_MANUELL_GODKJENNING
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
                is ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT -> ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT
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

fun FregStatusOgStatsborgerskap.toKanStarteOppfolging(): FregStatusSjekkResultat {
    val euEllerEøsBorger = this.statsborgerskap.any { eeaLand.contains(it) }
    val erGbrStatsborger = this.statsborgerskap.any { it === "GBR" }

    return when (this.fregStatus) {
        ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven -> FREG_STATUS_OK
        ForenkletFolkeregisterStatus.dNummer -> {
            if (euEllerEøsBorger || erGbrStatsborger)
                FREG_STATUS_OK
            else
                FREG_STATUS_KREVER_MANUELL_GODKJENNING
        }
        ForenkletFolkeregisterStatus.forsvunnet,
        ForenkletFolkeregisterStatus.opphoert -> IKKE_LOVLIG_OPPHOLD
        ForenkletFolkeregisterStatus.ikkeBosatt -> FREG_STATUS_KREVER_MANUELL_GODKJENNING
        ForenkletFolkeregisterStatus.doedIFolkeregisteret -> DOD
        ForenkletFolkeregisterStatus.ukjent -> UKJENT_STATUS_FOLKEREGISTERET
        ForenkletFolkeregisterStatus.ingen_status -> INGEN_STATUS_FOLKEREGISTERET
    }
}
