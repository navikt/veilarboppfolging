package no.nav.fo.veilarbsituasjon.rest.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;

@Data
@Accessors(chain = true)
public class OppfolgingStatus {
    public String fnr;
    public boolean reservasjonKRR;
    public boolean manuell;
    public boolean underOppfolging;
    public boolean vilkarMaBesvares;
    public Date oppfolgingUtgang;
    private boolean kanStarteOppfolging;
    private AvslutningStatus avslutningStatus;
    private List<OppfolgingPeriodeDTO> oppfolgingsPerioder;
}