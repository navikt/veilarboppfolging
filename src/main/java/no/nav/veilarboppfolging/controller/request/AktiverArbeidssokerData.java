package no.nav.veilarboppfolging.controller.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import no.nav.veilarboppfolging.domain.Innsatsgruppe;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AktiverArbeidssokerData {
    Fnr fnr;
    Innsatsgruppe innsatsgruppe;
}
