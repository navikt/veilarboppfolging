package no.nav.fo.veilarboppfolging.kafka;

import io.vavr.control.Try;
import lombok.val;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.OppfolgingFeedRepository;
import no.nav.fo.veilarboppfolging.domain.AktorId;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingKafkaDTO;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.bouncycastle.util.Strings;
import org.junit.Test;
import org.slf4j.MDC;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static no.nav.log.LogFilter.PREFERRED_NAV_CALL_ID_HEADER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class OppfolgingStatusKafkaProducerTest {

    @Test
    public void skal_publisere_all_brukere() {
        val repoMock = mock(OppfolgingFeedRepository.class);
        val producer = createMockProducer(repoMock);

        long antallBrukere = 200_000L;
        when(repoMock.hentAntallBrukere()).thenReturn(Optional.of(antallBrukere));
        when(repoMock.hentOppfolgingStatus(anyInt())).thenReturn(createTestBrukere(1000));

        int actualOffset = producer.publiserAlleBrukere();
        int expectedOffset = 201_000;
        assertThat(actualOffset).isEqualTo(expectedOffset);
    }


    @Test
    public void skal_fallbacke_til_mdc_job_id() {
        String jobId = "test";
        MDC.put("jobId", jobId);
        byte[] correlationId = OppfolgingStatusKafkaProducer.getCorrelationIdAsBytes();
        String id = Strings.fromByteArray(correlationId);
        assertThat(id).isEqualTo(jobId);
    }

    @Test
    public void skal_fallbacke_til_mdc_call_id() {
        String jobId = "test";
        MDC.put(PREFERRED_NAV_CALL_ID_HEADER_NAME, jobId);
        byte[] correlationId = OppfolgingStatusKafkaProducer.getCorrelationIdAsBytes();
        String id = Strings.fromByteArray(correlationId);
        assertThat(id).isEqualTo(jobId);
    }

    @Test
    public void skal_fallbacke_til_generert_id() {
        byte[] correlationId = OppfolgingStatusKafkaProducer.getCorrelationIdAsBytes();
        String id = Strings.fromByteArray(correlationId);
        assertThat(id).isNotBlank();
        assertThat(id).isNotEmpty();
    }

    @Test
    public void skal_returnere_feilresultat_om_bruker_ikke_har_oppfolgingsstatus() {
        val feedRepoMock = mock(OppfolgingFeedRepository.class);
        OppfolgingStatusKafkaProducer producer = createMockProducer(feedRepoMock);
        when(feedRepoMock.hentOppfolgingStatus(anyString())).thenReturn(Try.failure(new IllegalStateException()));

        Try<OppfolgingKafkaDTO> result = producer.send(testId());
        assertThat(result.isFailure()).isTrue();
    }

    private static AktorId testId() {
        return new AktorId("test");
    }

    private static OppfolgingStatusKafkaProducer createMockProducer(OppfolgingFeedRepository repoMock) {
        val kafkaMock = mock(KafkaProducer.class);
        when(kafkaMock.send(any(ProducerRecord.class))).thenReturn(mock(Future.class));

        return new OppfolgingStatusKafkaProducer(
                kafkaMock,
                repoMock,
                "test"
        );
    }

    private List<OppfolgingKafkaDTO> createTestBrukere(int antall) {
        return IntStream
                .range(0, antall)
                .mapToObj(n ->
                        OppfolgingKafkaDTO
                                .builder()
                                .aktoerid(String.valueOf(n))
                                .build()
                )
                .collect(toList());
    }
}