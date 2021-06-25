package no.nav.veilarboppfolging.domain;

import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Arrays;
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

    private OppfolgingsperiodeEntity tilPeriode(ZonedDateTime sluttDato) {
        return OppfolgingsperiodeEntity.builder()
                .sluttDato(sluttDato)
                .build();
    }
    
    @Test
    public void getOppfolgingUtgang_returnererSisteDatoHvisFlerePerioderFinnes() {
        ZonedDateTime tidligsteDato = ZonedDateTime.now();
        ZonedDateTime sisteDato = tidligsteDato.plusSeconds(1);
        OppfolgingStatusData setOppfolgingsperioder = new OppfolgingStatusData().setOppfolgingsperioder(lagPerioder(sisteDato, null, tidligsteDato));
        assertThat(setOppfolgingsperioder.getOppfolgingUtgang(), equalTo(sisteDato));
    }

    private List<OppfolgingsperiodeEntity> lagPerioder(ZonedDateTime... sluttDatoer) {
        return Arrays.stream(sluttDatoer).map(this::tilPeriode).collect(toList());
    }

}
