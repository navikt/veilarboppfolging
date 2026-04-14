package no.nav.veilarboppfolging.oppfolgingsbruker.inngang

enum class KanStarteOppfolgingEksterneDto {
    JA,
    JA_MED_MANUELL_GODKJENNING_PGA_UNDER_18,
    JA_MED_MANUELL_GODKJENNING_PGA_IKKE_BOSATT,
    JA_MED_MANUELL_GODKJENNING_PGA_DNUMMER_IKKE_EOS_GBR,
    ALLEREDE_UNDER_OPPFOLGING,
    DOD,
    IKKE_LOVLIG_OPPHOLD,
    UKJENT_STATUS_FOLKEREGISTERET,
    INGEN_STATUS_FOLKEREGISTERET;

    companion object {
        fun sjekkKanStarteOppfolgingPaBrukerForEksterne(
            erBrukerUnderOppfolging: Lazy<ErBrukerUnderOppfolging>,
            fregStatusSjekkResultat: Lazy<FregStatusSjekkResultat>
        ): KanStarteOppfolgingEksterneDto {

            return when (fregStatusSjekkResultat.value){
                is FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_UNDER_18 -> JA_MED_MANUELL_GODKJENNING_PGA_UNDER_18
                is DOD -> DOD
                is IKKE_LOVLIG_OPPHOLD -> IKKE_LOVLIG_OPPHOLD
                is INGEN_STATUS_FOLKEREGISTERET -> INGEN_STATUS_FOLKEREGISTERET
                is UKJENT_STATUS_FOLKEREGISTERET -> UKJENT_STATUS_FOLKEREGISTERET
                is FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_DNUMMER_IKKE_EOS_GBR -> JA_MED_MANUELL_GODKJENNING_PGA_DNUMMER_IKKE_EOS_GBR
                is FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_IKKE_BOSATT -> JA_MED_MANUELL_GODKJENNING_PGA_IKKE_BOSATT
                is FREG_STATUS_OK -> {
                    when (erBrukerUnderOppfolging.value) {
                        is ALLEREDE_UNDER_OPPFOLGING,
                        ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT -> ALLEREDE_UNDER_OPPFOLGING
                        is OPPFOLGING_OK -> JA
                    }
                }
            }
        }
    }
}