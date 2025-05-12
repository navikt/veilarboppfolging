package no.nav.veilarboppfolging.client.digdir_krr;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain = true)
public class KrrPersonerResponseDto {
    Map<String, DigdirKontaktinfo> personer;
    Map<String, String> feil;

    public KRRData assertSinglePersonToKrrData() {
        if (feil != null) throw new RuntimeException(String.format("Kunne ikke hente kontaktinfo fra KRR, feil: %s" + feil));
        if (personer == null || personer.size() != 1) throw new IllegalStateException("Fant ikke person i response fra KRR");
        var key = personer.keySet().stream().findFirst();
        if (key.isEmpty()) throw new IllegalStateException("Fant ingen keys (personidenter) i response fra KRR");
        return personer.get(key.get()).toKrrData();
    }
}