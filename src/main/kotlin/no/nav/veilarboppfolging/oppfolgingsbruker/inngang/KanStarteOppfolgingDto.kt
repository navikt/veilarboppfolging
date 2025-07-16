package no.nav.veilarboppfolging.oppfolgingsbruker.inngang

import no.nav.veilarboppfolging.client.pdl.ForenkletFolkeregisterStatus
import no.nav.veilarboppfolging.client.pdl.FregStatusOgStatsborgerskap
import no.nav.veilarboppfolging.client.pdl.eeaLand
import no.nav.veilarboppfolging.controller.TilgangResultat

sealed class KanStarteOppfolgingSjekk {
    fun oppfolgingSjekk(erBrukerUnderOppfolging: Lazy<ErBrukerUnderOppfolging>,
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
            is FREG_STATUS_KREVER_MANUELL_GODKJENNING -> {}
        }

        return when(erBrukerUnderOppfolging.value){
            ALLEREDE_UNDER_OPPFOLGING -> KanStarteOppfolgingDto.kanStarteOppfolging(erBrukerUnderOppfolging.value)
            ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT -> {
                if(fregStatusSjekkResultat.value is FREG_STATUS_KREVER_MANUELL_GODKJENNING){
                    return when (fregStatusSjekkResultat.value as FREG_STATUS_KREVER_MANUELL_GODKJENNING) {
                        is FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_IKKE_BOSATT ->
                            KanStarteOppfolgingDto.ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT_MEN_KREVER_MANUELL_GODKJENNING_PGA_IKKE_BOSATT
                        is FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_DNUMMER_IKKE_EOS_GBR ->
                            KanStarteOppfolgingDto.ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT_MEN_KREVER_MANUELL_GODKJENNING_PGA_DNUMMER_IKKE_EOS_GBR
                    }
                } else
                    KanStarteOppfolgingDto.ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT
            }
            OPPFOLGING_OK -> {
               if(fregStatusSjekkResultat.value is FREG_STATUS_KREVER_MANUELL_GODKJENNING) {
                   when (fregStatusSjekkResultat.value as FREG_STATUS_KREVER_MANUELL_GODKJENNING) {
                       is FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_IKKE_BOSATT ->
                           KanStarteOppfolgingDto.JA_MED_MANUELL_GODKJENNING_PGA_IKKE_BOSATT
                       is FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_DNUMMER_IKKE_EOS_GBR ->
                           KanStarteOppfolgingDto.JA_MED_MANUELL_GODKJENNING_PGA_DNUMMER_IKKE_EOS_GBR
                   }
                }
                else {
                    KanStarteOppfolgingDto.JA
                }
            }
        }
    }
}

sealed class ErBrukerUnderOppfolging(): KanStarteOppfolgingSjekk()
object OPPFOLGING_OK: ErBrukerUnderOppfolging()
object ALLEREDE_UNDER_OPPFOLGING: ErBrukerUnderOppfolging()
object ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT: ErBrukerUnderOppfolging()

sealed class VeilederHarTilgang: KanStarteOppfolgingSjekk()
object TILGANG_OK: VeilederHarTilgang()
object IKKE_TILGANG_FORTROLIG_ADRESSE: VeilederHarTilgang()
object IKKE_TILGANG_STRENGT_FORTROLIG_ADRESSE: VeilederHarTilgang()
object IKKE_TILGANG_EGNE_ANSATTE: VeilederHarTilgang()
object IKKE_TILGANG_ENHET: VeilederHarTilgang()
object IKKE_TILGANG_MODIA: VeilederHarTilgang()

sealed class FregStatusSjekkResultat: KanStarteOppfolgingSjekk()
object FREG_STATUS_OK: FregStatusSjekkResultat()
sealed class FREG_STATUS_KREVER_MANUELL_GODKJENNING: FregStatusSjekkResultat()
object FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_IKKE_BOSATT: FREG_STATUS_KREVER_MANUELL_GODKJENNING()
object FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_DNUMMER_IKKE_EOS_GBR: FREG_STATUS_KREVER_MANUELL_GODKJENNING()
object DOD: FregStatusSjekkResultat()
object IKKE_LOVLIG_OPPHOLD: FregStatusSjekkResultat()
object UKJENT_STATUS_FOLKEREGISTERET: FregStatusSjekkResultat()
object INGEN_STATUS_FOLKEREGISTERET: FregStatusSjekkResultat()

enum class KanStarteOppfolgingDto {
    JA,
    JA_MED_MANUELL_GODKJENNING_PGA_IKKE_BOSATT,
    JA_MED_MANUELL_GODKJENNING_PGA_DNUMMER_IKKE_EOS_GBR,
    ALLEREDE_UNDER_OPPFOLGING,
    ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT,
    ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT_MEN_KREVER_MANUELL_GODKJENNING_PGA_IKKE_BOSATT,
    ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT_MEN_KREVER_MANUELL_GODKJENNING_PGA_DNUMMER_IKKE_EOS_GBR,
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
                is FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_IKKE_BOSATT -> JA_MED_MANUELL_GODKJENNING_PGA_IKKE_BOSATT
                is FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_DNUMMER_IKKE_EOS_GBR -> JA_MED_MANUELL_GODKJENNING_PGA_DNUMMER_IKKE_EOS_GBR
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
                FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_DNUMMER_IKKE_EOS_GBR
        }
        ForenkletFolkeregisterStatus.forsvunnet,
        ForenkletFolkeregisterStatus.opphoert -> IKKE_LOVLIG_OPPHOLD
        ForenkletFolkeregisterStatus.ikkeBosatt -> FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_IKKE_BOSATT
        ForenkletFolkeregisterStatus.doedIFolkeregisteret -> DOD
        ForenkletFolkeregisterStatus.ukjent -> UKJENT_STATUS_FOLKEREGISTERET
        ForenkletFolkeregisterStatus.ingen_status -> INGEN_STATUS_FOLKEREGISTERET
    }
}
