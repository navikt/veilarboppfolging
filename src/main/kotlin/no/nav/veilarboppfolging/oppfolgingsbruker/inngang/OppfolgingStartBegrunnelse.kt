package no.nav.veilarboppfolging.oppfolgingsbruker.inngang

import no.nav.veilarboppfolging.kafka.dto.StartetBegrunnelseDTO

enum class OppfolgingStartBegrunnelse {
    ARBEIDSSOKER_REGISTRERING,
    ARENA_SYNC_ARBS,
    ARENA_SYNC_IARBS,
    MANUELL_REGISTRERING_VEILEDER,
    REAKTIVERT_OPPFØLGING;

    fun toStartetBegrunnelseDTO(): StartetBegrunnelseDTO {
        if (this == ARBEIDSSOKER_REGISTRERING || this == ARENA_SYNC_ARBS) {
            return StartetBegrunnelseDTO.ARBEIDSSOKER
        } else { // Reativer er sykmeldt fordi arbeidsøkere automatisk er under oppfølging
            return StartetBegrunnelseDTO.SYKEMELDT_MER_OPPFOLGING
        }
    }
}