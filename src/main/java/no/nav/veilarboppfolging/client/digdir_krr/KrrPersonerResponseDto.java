package no.nav.veilarboppfolging.client.digdir_krr;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

@Data
@Accessors(chain = true)
@Slf4j
public class KrrPersonerResponseDto {
    Map<String, DigdirKontaktinfo> personer;
    Map<String, String> feil;

    public Optional<KRRData> assertSinglePersonToKrrData() {
        if (feil != null) {
            log.warn("Kunne ikke hente kontaktinfo fra KRR, feil: {}", feil);
            return Optional.empty();
        }
        if (personer == null || personer.size() != 1) {
            log.warn("Fant ikke person i response fra KRR");
            return Optional.empty();
        }
        var key = personer.keySet().stream().findFirst();
        return Optional.of(personer.get(key.get()).toKrrData());
    }
}