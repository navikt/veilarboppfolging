package no.nav.veilarboppfolging.repository.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import no.nav.veilarboppfolging.domain.StartetAvType;
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingStartBegrunnelse;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@AllArgsConstructor
public class OppfolgingsperiodeEntity {
    UUID uuid;
    String aktorId;
    String veileder;
    ZonedDateTime startDato;
    ZonedDateTime sluttDato;
    String begrunnelse;
    List<KvpPeriodeEntity> kvpPerioder;
    OppfolgingStartBegrunnelse startetBegrunnelse;
    @Nullable
    String startetAv;
    @Nullable
    StartetAvType startetAvType;
}
