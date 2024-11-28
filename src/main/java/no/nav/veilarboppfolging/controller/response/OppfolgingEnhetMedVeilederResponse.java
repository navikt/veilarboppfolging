package no.nav.veilarboppfolging.controller.response;

import lombok.Data;
import lombok.Value;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class OppfolgingEnhetMedVeilederResponse {
    Oppfolgingsenhet oppfolgingsenhet;
    String veilederId;
    String formidlingsgruppe;
    String servicegruppe;
    String hovedmaalkode;

    @Value
    public static class Oppfolgingsenhet {
        String navn;
        String enhetId;
    }
}
