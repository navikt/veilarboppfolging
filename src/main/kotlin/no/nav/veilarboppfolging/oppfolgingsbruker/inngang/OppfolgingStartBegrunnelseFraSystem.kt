package no.nav.veilarboppfolging.oppfolgingsbruker.inngang

enum class OppfolgingStartBegrunnelseFraSystem {
    SYKMELDT_UTEN_ARBEIDSGIVER_4_UKER;

    fun toOppfolgingStartBegrunnelse(): OppfolgingStartBegrunnelse {
        return when (this) {
            SYKMELDT_UTEN_ARBEIDSGIVER_4_UKER -> OppfolgingStartBegrunnelse.SYKMELDT_UTEN_ARBEIDSGIVER_4_UKER
        }
    }
}