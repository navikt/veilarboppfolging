package no.nav.veilarboppfolging.domain;

import lombok.Value;
import no.nav.common.types.identer.AktorId;

@Value
public class RepubliserOppfolgingsperioderRequest {
    public AktorId aktorId;
}
