package no.nav.veilarboppfolging.repository.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import no.nav.veilarboppfolging.oppfolgingsbruker.OppfolgingStartBegrunnelse;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class OppfolgingsperiodeEntity {
    public UUID uuid;
    public String aktorId;
    public String veileder;
    public ZonedDateTime startDato;
    public ZonedDateTime sluttDato;
    public String begrunnelse;
    public List<KvpPeriodeEntity> kvpPerioder;
    public OppfolgingStartBegrunnelse startetBegrunnelse;
}
