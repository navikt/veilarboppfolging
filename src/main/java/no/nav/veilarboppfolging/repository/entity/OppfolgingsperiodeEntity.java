package no.nav.veilarboppfolging.repository.entity;

import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class OppfolgingsperiodeEntity {
    UUID uuid;
    String aktorId;
    String veileder;
    ZonedDateTime startDato;
    ZonedDateTime sluttDato;
    String begrunnelse;
    List<KvpPeriodeEntity> kvpPerioder;
    OppfolgingStartBegrunnelse startetBegrunnelse;
}
