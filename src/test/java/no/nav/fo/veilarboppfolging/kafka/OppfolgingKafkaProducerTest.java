package no.nav.fo.veilarboppfolging.kafka;

import lombok.val;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.OppfolgingFeedRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingKafkaFeiletMeldingRepository;
import no.nav.fo.veilarboppfolging.domain.AktorId;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.bouncycastle.util.Strings;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.MDC;

import java.util.concurrent.Future;

import static no.nav.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class OppfolgingKafkaProducerTest {

    @Test
    public void skal_fallbacke_til_mdc_job_id() {
        String jobId = "test";
        MDC.put("jobId", jobId);
        byte[] correlationId = OppfolgingKafkaProducer.getCorrelationIdAsBytes();
        String id = Strings.fromByteArray(correlationId);
        assertThat(id).isEqualTo(jobId);
    }

    @Test
    public void skal_fallbacke_til_mdc_call_id() {
        String jobId = "test";
        MDC.put(PREFERRED_NAV_CALL_ID_HEADER_NAME, jobId);
        byte[] correlationId = OppfolgingKafkaProducer.getCorrelationIdAsBytes();
        String id = Strings.fromByteArray(correlationId);
        assertThat(id).isEqualTo(jobId);
    }

    @Test
    public void skal_fallbacke_til_generert_id() {
        byte[] correlationId = OppfolgingKafkaProducer.getCorrelationIdAsBytes();
        String id = Strings.fromByteArray(correlationId);
        assertThat(id).isNotBlank();
        assertThat(id).isNotEmpty();
    }

    @Ignore
    @Test
    public void skal_feile_om_oppfolgingsstatus_for_bruker_ikke_finnes_i_repo() {
        val repoMock = mock(OppfolgingKafkaFeiletMeldingRepository.class);
        val feedRepoMock = mock(OppfolgingFeedRepository.class);
        OppfolgingKafkaProducer producer = createMockProducer(repoMock, feedRepoMock);

        producer.send(testId());
    }

    private static AktorId testId() {
        return new AktorId("test");
    }

    private static OppfolgingKafkaProducer createMockProducer(OppfolgingKafkaFeiletMeldingRepository kafkaRepoMock, OppfolgingFeedRepository feedRepositoryMock) {
        val kafkaMock = mock(KafkaProducer.class);
        when(kafkaMock.send(any(ProducerRecord.class))).thenReturn(mock(Future.class));
        return new OppfolgingKafkaProducer(
                kafkaMock,
                feedRepositoryMock,
                kafkaRepoMock,
                mock(AktorService.class),
                "test");
    }
}