package no.nav.veilarboppfolging.oppfolgingsbruker.inngang

enum class KanStarteOppfolgingEksterneDto {
    JA,
    ALLEREDE_UNDER_OPPFOLGING,
    ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT,
    DOD,
    IKKE_LOVLIG_OPPHOLD,
    UKJENT_STATUS_FOLKEREGISTERET,
    INGEN_STATUS_FOLKEREGISTERET;

    companion object {
        fun sjekkKanStarteOppfolgingPaBrukerForEksterne(
            erBrukerUnderOppfolging: Lazy<ErBrukerUnderOppfolging>,
            fregStatusSjekkResultat: Lazy<FregStatusSjekkResultat>
        ): KanStarteOppfolgingEksterneDto {

            when(fregStatusSjekkResultat.value){
                is DOD -> return DOD
                is IKKE_LOVLIG_OPPHOLD -> return IKKE_LOVLIG_OPPHOLD
                is INGEN_STATUS_FOLKEREGISTERET -> return INGEN_STATUS_FOLKEREGISTERET
                is UKJENT_STATUS_FOLKEREGISTERET -> return UKJENT_STATUS_FOLKEREGISTERET
                is FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_DNUMMER_IKKE_EOS_GBR -> return IKKE_LOVLIG_OPPHOLD
                is FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_IKKE_BOSATT -> return IKKE_LOVLIG_OPPHOLD
                is FREG_STATUS_OK -> {}
            }

            return when (erBrukerUnderOppfolging.value) {
                is ALLEREDE_UNDER_OPPFOLGING -> ALLEREDE_UNDER_OPPFOLGING
                is ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT -> ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT
                is OPPFOLGING_OK -> JA
            }
        }
    }
}