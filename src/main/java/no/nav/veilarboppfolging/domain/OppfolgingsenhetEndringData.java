package no.nav.veilarboppfolging.domain;

import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder
public class OppfolgingsenhetEndringData {
    private String enhet;
    private Date endretDato;
}
