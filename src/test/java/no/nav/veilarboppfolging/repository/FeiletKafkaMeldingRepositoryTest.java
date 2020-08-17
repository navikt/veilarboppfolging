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

        List<FeiletKafkaMelding> feiledeVedtakSendt = feiletKafkaMeldingRepository.hentFeiledeKafkaMeldinger(10);

        assertEquals(1, feiledeVedtakSendt.size());
        assertEquals(feiledeVedtakSendt.get(0).getTopicName(), "topic");
        assertEquals(feiledeVedtakSendt.get(0).getMessageKey(), "key");
        assertEquals(feiledeVedtakSendt.get(0).getJsonPayload(), "payload");
    }

    @Test
    public void skal_kun_hente_1_melding() {
        feiletKafkaMeldingRepository.lagreFeiletKafkaMelding("topic", "key", "payload");
        feiletKafkaMeldingRepository.lagreFeiletKafkaMelding("topic", "key1", "payload1");

        List<FeiletKafkaMelding> feiledeVedtakSendt = feiletKafkaMeldingRepository.hentFeiledeKafkaMeldinger(1);

        assertEquals(1, feiledeVedtakSendt.size());
    }

    @Test
    public void skal_slette_feilet_melding() {
        feiletKafkaMeldingRepository.lagreFeiletKafkaMelding("topic", "key", "payload");
        List<FeiletKafkaMelding> feiletKafkaMeldinger = feiletKafkaMeldingRepository.hentFeiledeKafkaMeldinger(10);
        feiletKafkaMeldingRepository.slettFeiletKafkaMelding(feiletKafkaMeldinger.get(0).getId());

        assertTrue(feiletKafkaMeldingRepository.hentFeiledeKafkaMeldinger(10).isEmpty());
    }

}
