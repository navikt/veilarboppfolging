package no.nav.veilarboppfolging.domain;

import lombok.Value;

@Value
public class AvsluttOppfolgingsperiodePayload {
    String aktorId;
    String begrunnelse;
    String oppfolgingsperiodeUuid;
}
