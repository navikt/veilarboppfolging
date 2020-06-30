package no.nav.veilarboppfolging.internal;

import lombok.Value;

import java.util.List;

@Value
public class OppfolgingEnhetPageDTO {
    int page_number;
    Integer page_next;
    int page_number_total;
    List<OppfolgingEnhetDTO> users;
}
