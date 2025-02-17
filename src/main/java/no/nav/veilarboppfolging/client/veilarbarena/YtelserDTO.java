package no.nav.veilarboppfolging.client.veilarbarena;

import lombok.Data;
import lombok.Value;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.util.List;

@Value
public class YtelserDTO {
    public List<Vedtak> vedtak;
    public List<Ytelseskontrakt> ytelser;

    @Data
    @Accessors(chain = true)
    public static class Vedtak {
        public String type;
        public String status;
        public String aktivitetsfase;
        public String rettighetsgruppe;
        public LocalDate fraDato;
        public LocalDate tilDato;
    }

    @Data
    @Accessors(chain = true)
    public static class Ytelseskontrakt {
        public String type;
        public String status;
        public LocalDate motattDato;
        public LocalDate fraDato;
        public LocalDate tilDato;
    }
}
