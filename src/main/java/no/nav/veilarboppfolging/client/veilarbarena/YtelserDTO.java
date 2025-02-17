package no.nav.veilarboppfolging.client.veilarbarena;

import lombok.Data;
import lombok.Value;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.util.List;

@Value
public class YtelserDTO {
    List<Vedtak> vedtak;
    List<Ytelseskontrakt> ytelser;

    @Data
    @Accessors(chain = true)
    public static class Vedtak {
        String type;
        String status;
        String aktivitetsfase;
        String rettighetsgruppe;
        LocalDate fraDato;
        LocalDate tilDato;
    }

    @Data
    @Accessors(chain = true)
    public static class Ytelseskontrakt {
        String type;
        String status;
        LocalDate motattDato;
        LocalDate fraDato;
        LocalDate tilDato;
    }
}
