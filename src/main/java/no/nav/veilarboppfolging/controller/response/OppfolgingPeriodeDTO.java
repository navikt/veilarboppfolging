package no.nav.veilarboppfolging.controller.response;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public record OppfolgingPeriodeDTO(
    UUID uuid,
    String aktorId,
    String veileder,
    ZonedDateTime startDato,
    ZonedDateTime sluttDato,
    String begrunnelse,
    List<KvpPeriodeDTO> kvpPerioder
){}
