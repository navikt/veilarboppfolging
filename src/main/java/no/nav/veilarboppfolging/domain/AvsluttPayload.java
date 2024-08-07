package no.nav.veilarboppfolging.domain;

import lombok.Data;

import java.util.List;

@Data
public class AvsluttPayload {
    public List<String> aktorIds;
    public String begrunnelse;
}
