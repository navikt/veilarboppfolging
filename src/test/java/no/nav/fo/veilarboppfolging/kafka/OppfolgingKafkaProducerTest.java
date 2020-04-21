package no.nav.fo.veilarboppfolging.kafka;

import org.bouncycastle.util.Strings;
import org.junit.Test;
import org.slf4j.MDC;

import static no.nav.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;

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

}