package no.nav.fo.veilarboppfolging.db;

import no.nav.fo.veilarboppfolging.domain.AktorId;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static no.nav.fo.veilarboppfolging.config.JndiLocalContextConfig.setupInMemoryDatabase;
import static org.assertj.core.api.Assertions.assertThat;

public class OppfolgingKafkaFeiletMeldingRepositoryTest {

    private OppfolgingKafkaFeiletMeldingRepository repository;
    private static final AktorId TEST_ID = new AktorId("test");

    @Before
    public void setUp() {
        JdbcTemplate db = new JdbcTemplate(setupInMemoryDatabase());
        repository = new OppfolgingKafkaFeiletMeldingRepository(db);
    }

    @Test
    public void skal_inserte_aktor_id() {
        repository.insertFeiletMelding(TEST_ID);
        List<AktorId> aktorIds = repository.hentFeiledeMeldinger();
        assertThat(aktorIds).contains(TEST_ID);
    }

    @Test
    public void skal_slette_aktor_id() {
        repository.insertFeiletMelding(TEST_ID);
        assertThat(repository.hentFeiledeMeldinger()).isNotEmpty();
        repository.deleteFeiletMelding(TEST_ID);
        assertThat(repository.hentFeiledeMeldinger()).isEmpty();
    }

    @Test
    public void skal_ikke_feile_ved_sletting_om_aktoer_id_ikke_finnes() {
        repository.deleteFeiletMelding(new AktorId("foo"));
    }

}