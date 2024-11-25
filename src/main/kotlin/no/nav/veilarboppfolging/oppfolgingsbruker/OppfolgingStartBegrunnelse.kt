package no.nav.veilarboppfolging.oppfolgingsbruker

import no.nav.veilarboppfolging.kafka.dto.StartetBegrunnelseDTO

enum class OppfolgingStartBegrunnelse {
    ARBEIDSSOKER_REGISTRERING,
    REAKTIVERT,
    SYKMELDT_MER_OPPFOLGING,
    ARENA_SYNC,  // TODO: Fjerne etter opprydding i databasen
    ARENA_SYNC_ARBS,
    ARENA_SYNC_IARBS,
    MANUELL_REGISTRERING;

    fun toStartetBegrunnelseDTO(): StartetBegrunnelseDTO {
        if (this == OppfolgingStartBegrunnelse.ARBEIDSSOKER_REGISTRERING || this == OppfolgingStartBegrunnelse.ARENA_SYNC_ARBS) {
            return StartetBegrunnelseDTO.ARBEIDSSOKER
        } else { // Reativer er sykmeldt fordi arbeidsøkere automatisk er under oppfølging
            return StartetBegrunnelseDTO.SYKEMELDT_MER_OPPFOLGING
        }
    }
}
