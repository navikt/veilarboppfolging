package no.nav.fo.veilarbsituasjon.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import static java.util.Comparator.naturalOrder;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Data
@Accessors(chain = true)
public class OppfolgingStatusData {
    public String fnr;
    public String veilederId;
    public boolean reservasjonKRR;
    public boolean manuell;
    public boolean underOppfolging;
    public boolean vilkarMaBesvares;
    public boolean kanStarteOppfolging;
    public AvslutningStatusData avslutningStatusData;
    public List<Oppfolgingsperiode> oppfolgingsperioder = Collections.emptyList();
    
    public Date getOppfolgingUtgang() {
        return oppfolgingsperioder.stream().map(Oppfolgingsperiode::getSluttDato).filter(Objects::nonNull).max(naturalOrder()).orElse(null);
    }
    
}