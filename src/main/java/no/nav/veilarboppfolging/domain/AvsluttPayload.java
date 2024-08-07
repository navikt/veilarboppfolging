package no.nav.veilarboppfolging.domain;

import lombok.Value;

import java.util.List;

@Value
public class AvsluttPayload {
    public List<String> aktorIds;
    public String begrunnelse;
}
