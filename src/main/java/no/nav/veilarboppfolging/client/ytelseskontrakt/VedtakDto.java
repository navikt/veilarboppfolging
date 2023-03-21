package no.nav.veilarboppfolging.client.ytelseskontrakt;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class VedtakDto {
    String vedtakstype;
    String status;
    String aktivitetsfase;
    String rettighetsgruppe;
    Dato fradato;
    Dato tildato;
}
