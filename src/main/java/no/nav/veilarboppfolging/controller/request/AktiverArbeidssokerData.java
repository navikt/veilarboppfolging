package no.nav.veilarboppfolging.controller.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AktiverArbeidssokerData {
    Fnr fnr;
    Innsatsgruppe innsatsgruppe;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Fnr {
        private String fnr;
    }
}
