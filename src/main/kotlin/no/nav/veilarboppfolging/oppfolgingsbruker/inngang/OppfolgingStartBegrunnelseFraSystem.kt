package no.nav.veilarboppfolging.oppfolgingsbruker.inngang

enum class OppfolgingStartBegrunnelseFraSystem {
    SYKMELDT_UTEN_ARBEIDSGIVER_4_UKER,
    AAP_SØKNAD,
    ADMIN_START_OPPFOLGING_MED_FORRIGE_AO_KONTOR;

    fun toOppfolgingStartBegrunnelse(): OppfolgingStartBegrunnelse {
        return when (this) {
            SYKMELDT_UTEN_ARBEIDSGIVER_4_UKER -> OppfolgingStartBegrunnelse.SYKMELDT_UTEN_ARBEIDSGIVER_4_UKER
            AAP_SØKNAD -> OppfolgingStartBegrunnelse.AAP_SØKNAD
            ADMIN_START_OPPFOLGING_MED_FORRIGE_AO_KONTOR -> OppfolgingStartBegrunnelse.ADMIN_START_OPPFOLGING_MED_FORRIGE_AO_KONTOR
        }
    }
}