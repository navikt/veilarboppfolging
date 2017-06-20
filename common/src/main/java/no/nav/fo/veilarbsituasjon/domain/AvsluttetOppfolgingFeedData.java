package no.nav.fo.veilarbsituasjon.domain;

import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder
public class AvsluttetOppfolgingFeedData {

    public String aktoerid;
    public Date sluttdato;
    public Date oppdatert;
}
