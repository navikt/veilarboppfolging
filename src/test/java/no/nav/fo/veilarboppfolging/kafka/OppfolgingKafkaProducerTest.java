package no.nav.fo.veilarboppfolging.kafka;

import lombok.val;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.OppfolgingFeedRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingKafkaFeiletMeldingRepository;
import no.nav.fo.veilarboppfolging.domain.AktorId;
import org.bouncycastle.util.Strings;
import org.junit.Test;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

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

    @Test
    public void skal_slette_melding_i_database_ved_suksess() {
        val repoMock = mock(OppfolgingKafkaFeiletMeldingRepository.class);
        OppfolgingKafkaProducer producer = createMockProducer(repoMock);

        producer.onSuccess(testId()).onSuccess(mock(SendResult.class));
        verify(repoMock, times(1)).deleteFeiletMelding(any());
    }

    @Test
    public void skal_inserte_feilmelding_ved_error() {
        val repoMock = mock(OppfolgingKafkaFeiletMeldingRepository.class);
        OppfolgingKafkaProducer producer = createMockProducer(repoMock);

        producer.onError(testId()).onFailure(new RuntimeException());
        verify(repoMock, times(1)).insertFeiletMelding(any());
    }

    @Test
    public void skal_feile_og_returnere_om_oppfolgingsstatus_for_bruker_ikke_finnes_i_repo() {
        val repoMock = mock(OppfolgingKafkaFeiletMeldingRepository.class);
        val producer = createMockProducer(repoMock);

        val future = producer.send(testId());
        assertThat(future.isCompletedExceptionally()).isTrue();
    }

    private static AktorId testId() {
        return new AktorId("test");
    }

    private static OppfolgingKafkaProducer createMockProducer(OppfolgingKafkaFeiletMeldingRepository kafkaRepoMock) {
        return new OppfolgingKafkaProducer(
                mock(KafkaTemplate.class),
                mock(OppfolgingFeedRepository.class),
                kafkaRepoMock,
                mock(AktorService.class)
        );
    }
}