package no.nav.veilarboppfolging.repository;

import no.nav.veilarboppfolging.domain.FeiletKafkaMelding;
import no.nav.veilarboppfolging.test.DbTestUtils;
import no.nav.veilarboppfolging.test.LocalH2Database;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FeiletKafkaMeldingRepositoryTest {

    private static FeiletKafkaMeldingRepository feiletKafkaMeldingRepository = new FeiletKafkaMeldingRepository(LocalH2Database.getDb());

    @Before
    public void cleanup() {
        DbTestUtils.cleanupTestDb();
    }

    @Test
    public void skal_lage_og_hente_melding() {
        feiletKafkaMeldingRepository.lagreFeiletKafkaMelding("topic", "key", "payload");

        List<FeiletKafkaMelding> feiledeVedtakSendt = feiletKafkaMeldingRepository.hentFeiledeKafkaMeldinger("topic");

        assertEquals(1, feiledeVedtakSendt.size());
        assertEquals(feiledeVedtakSendt.get(0).getMessageKey(), "key");
        assertEquals(feiledeVedtakSendt.get(0).getJsonPayload(), "payload");
    }

    @Test
    public void skal_hente_melding_fra_riktig_topic() {
        feiletKafkaMeldingRepository.lagreFeiletKafkaMelding("topic", "key", "payload");
        feiletKafkaMeldingRepository.lagreFeiletKafkaMelding("topic1", "key1", "payload1");
        feiletKafkaMeldingRepository.lagreFeiletKafkaMelding("topic2", "key2", "payload2");

        List<FeiletKafkaMelding> feiledeVedtakSendt = feiletKafkaMeldingRepository.hentFeiledeKafkaMeldinger("topic");

        assertEquals(1, feiledeVedtakSendt.size());
        assertEquals(feiledeVedtakSendt.get(0).getMessageKey(), "key");
        assertEquals(feiledeVedtakSendt.get(0).getJsonPayload(), "payload");
    }

    @Test
    public void skal_slette_feilet_melding() {
        feiletKafkaMeldingRepository.lagreFeiletKafkaMelding("topic", "key", "payload");
        List<FeiletKafkaMelding> feiletKafkaMeldinger = feiletKafkaMeldingRepository.hentFeiledeKafkaMeldinger("topic");
        feiletKafkaMeldingRepository.slettFeiletKafkaMelding(feiletKafkaMeldinger.get(0).getId());

        assertTrue(feiletKafkaMeldingRepository.hentFeiledeKafkaMeldinger("topic").isEmpty());
    }

}
