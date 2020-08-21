package no.nav.veilarboppfolging.domain;

import org.junit.Test;

import java.util.Date;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class OppfolgingStatusDataTest {

    @Test
    public void getOppfolgingUtgang_returnererNullHVisIngenPerioder() {
        assertThat(new OppfolgingStatusData().getOppfolgingUtgang(), nullValue());
    }
    
    @Test
    public void getOppfolgingUtgang_returnererNullHvisPeriodeUtenSluttdatoFinnes() {
        assertThat(new OppfolgingStatusData().setOppfolgingsperioder(asList(tilPeriode(null))).getOppfolgingUtgang(), nullValue());
    }

    private Oppfolgingsperiode tilPeriode(Date sluttDato) {
        return new Oppfolgingsperiode.OppfolgingsperiodeBuilder().sluttDato(sluttDato).build();
    }
    
    @Test
    public void getOppfolgingUtgang_returnererSisteDatoHvisFlerePerioderFinnes() {
        Date tidligsteDato = new Date();
        Date sisteDato = new Date(tidligsteDato.getTime() + 1);
        OppfolgingStatusData setOppfolgingsperioder = new OppfolgingStatusData().setOppfolgingsperioder(lagPerioder(sisteDato, null, tidligsteDato));
        assertThat(setOppfolgingsperioder.getOppfolgingUtgang(), equalTo(sisteDato));
    }

    private List<Oppfolgingsperiode> lagPerioder(Date... sluttDatoer) {
        return asList(sluttDatoer).stream().map(sluttDato -> tilPeriode(sluttDato)).collect(toList());
    }

}
