package no.nav.veilarboppfolging.repository.entity;

import no.nav.veilarboppfolging.kafka.dto.StartetBegrunnelseDTO;

public enum OppfolgingStartBegrunnelse {
    ARBEIDSSOKER_REGISTRERING,
    REAKTIVERT,
    SYKMELDT_MER_OPPFOLGING,
    ARENA_SYNC, // TODO: Fjerne etter opprydding i databasen
    ARENA_SYNC_ARBS,
    ARENA_SYNC_IARBS,
    NYTT_ARBEIDSSØKERREGISTER;

    public StartetBegrunnelseDTO toStartetBegrunnelseDTO() {
        if (this == ARBEIDSSOKER_REGISTRERING || this == ARENA_SYNC_ARBS || this == NYTT_ARBEIDSSØKERREGISTER) {
            return StartetBegrunnelseDTO.ARBEIDSSOKER;
        } else { // Reativer er sykmeldt fordi arbeidsøkere automatisk er under oppfølging
            return StartetBegrunnelseDTO.SYKEMELDT_MER_OPPFOLGING;
        }
    }
}
