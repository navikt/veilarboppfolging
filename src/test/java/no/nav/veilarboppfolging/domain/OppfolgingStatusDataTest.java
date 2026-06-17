package no.nav.veilarboppfolging.domain;

import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class OppfolgingStatusDataTest {

    @Test
    public void getOppfolgingUtgang_returnererNullHVisIngenPerioder() {
        assertThat(statusData(List.of()).getOppfolgingUtgang(), nullValue());
    }

    @Test
    public void getOppfolgingUtgang_returnererNullHvisPeriodeUtenSluttdatoFinnes() {
        assertThat(statusData(asList(tilPeriode(null))).getOppfolgingUtgang(), nullValue());
    }

    private OppfolgingsperiodeEntity tilPeriode(ZonedDateTime sluttDato) {
        return new OppfolgingsperiodeEntity(
                UUID.randomUUID(), "aktorId", null, ZonedDateTime.now(), sluttDato,
                null, null, null, null, null, null
        );
    }

    @Test
    public void getOppfolgingUtgang_returnererSisteDatoHvisFlerePerioderFinnes() {
        ZonedDateTime tidligsteDato = ZonedDateTime.now();
        ZonedDateTime sisteDato = tidligsteDato.plusSeconds(1);
        OppfolgingStatusData setOppfolgingsperioder = statusData(lagPerioder(sisteDato, null, tidligsteDato));
        assertThat(setOppfolgingsperioder.getOppfolgingUtgang(), equalTo(sisteDato));
    }

    private List<OppfolgingsperiodeEntity> lagPerioder(ZonedDateTime... sluttDatoer) {
        return Arrays.stream(sluttDatoer).map(this::tilPeriode).collect(toList());
    }

    private OppfolgingStatusData statusData(List<OppfolgingsperiodeEntity> perioder) {
        return new OppfolgingStatusData(
                "fnr", "aktorId", null, false, false, false, false, false, null, false,
                perioder, List.of(), false, null, null, null, null, null, null, null, null
        );
    }

}
