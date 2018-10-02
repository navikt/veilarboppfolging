package no.nav.fo.veilarboppfolging.services;

import no.nav.fo.IntegrasjonsTest;
import no.nav.fo.veilarboppfolging.domain.Iserv28;
import no.nav.fo.veilarboppfolging.mappers.ArenaBruker;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static java.time.ZonedDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;

public class Iserv28ServiceIntegrationTest extends IntegrasjonsTest {

    private static int nesteAktorId;

    @Inject
    private JdbcTemplate jdbcTemplate;

    private Iserv28Service iserv28Service;

    @Before
    public void setup() {
        iserv28Service = new Iserv28Service(jdbcTemplate, null, null);
    }

    @Test
    public void insertOgHentIservBruker() {
        ArenaBruker arenaBruker = new ArenaBruker();
        arenaBruker.setAktoerid("1234");
        arenaBruker.setFormidlingsgruppekode("ISERV");
        ZonedDateTime iservFraDato = now();
        arenaBruker.setIserv_fra_dato(iservFraDato);
        assertThat(iserv28Service.eksisterendeIservBruker(arenaBruker)).isNull();

        iserv28Service.filterereIservBrukere(arenaBruker);

        Iserv28 iserv28 = iserv28Service.eksisterendeIservBruker(arenaBruker);
        assertThat(iserv28).isNotNull();
        assertThat(iserv28.getAktor_Id()).isEqualTo("1234");
        assertThat(iserv28.getIservSiden()).isEqualTo(iservFraDato);
    }

    @Test
    public void finnBrukereMedIservI28Dager() {
        assertThat(iserv28Service.finnBrukereMedIservI28Dager()).isEmpty();

        insertIservBruker(now().minusDays(60));
        insertIservBruker(now().minusDays(27));
        insertIservBruker(now().minusDays(15));
        insertIservBruker(now());

        assertThat(iserv28Service.finnBrukereMedIservI28Dager()).hasSize(1);
    }

    private void insertIservBruker(ZonedDateTime iservFraDato) {
        ArenaBruker arenaBruker = new ArenaBruker();
        arenaBruker.setAktoerid(Integer.toString(nesteAktorId++));
        arenaBruker.setFormidlingsgruppekode("ISERV");
        arenaBruker.setIserv_fra_dato(iservFraDato);

        assertThat(iserv28Service.eksisterendeIservBruker(arenaBruker)).isNull();

        iserv28Service.filterereIservBrukere(arenaBruker);
    }

}