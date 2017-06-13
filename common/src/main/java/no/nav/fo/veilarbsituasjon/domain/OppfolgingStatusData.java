package no.nav.fo.veilarbsituasjon.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Accessors(chain = true)
public class OppfolgingStatusData {
    public String fnr;
    public boolean reservasjonKRR;
    public boolean manuell;
    public boolean underOppfolging;
    public boolean vilkarMaBesvares;
    public Date oppfolgingUtgang;
    private boolean kanStarteOppfolging;
}