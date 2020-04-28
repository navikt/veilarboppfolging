package no.nav.internal;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.fo.veilarboppfolging.db.OppfolgingFeedRepository;
import no.nav.fo.veilarboppfolging.domain.AktorId;
import no.nav.fo.veilarboppfolging.kafka.OppfolgingKafkaProducer;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingFeedDTO;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingKafkaDTO;
import no.nav.jobutils.JobUtils;
import no.nav.jobutils.RunningJob;
import org.apache.kafka.clients.producer.RecordMetadata;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toList;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static no.nav.internal.AuthorizationUtils.isBasicAuthAuthorized;
import static no.nav.jobutils.JobUtils.runAsyncJob;

@Slf4j
public class PopulerOppfolgingKafkaTopicServlet extends HttpServlet {

    private final OppfolgingKafkaProducer oppfolgingKafkaProducer;
    private final OppfolgingFeedRepository oppfolgingFeedRepository;

    @Inject
    public PopulerOppfolgingKafkaTopicServlet(OppfolgingKafkaProducer oppfolgingKafkaProducer,
                                              OppfolgingFeedRepository oppfolgingFeedRepository) {
        this.oppfolgingKafkaProducer = oppfolgingKafkaProducer;
        this.oppfolgingFeedRepository = oppfolgingFeedRepository;
    }

    @Override
    @SneakyThrows
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        if (isBasicAuthAuthorized(req)) {
            log.info("Hentet ut alle brukere under oppfolging");
            List<AktorId> aktorIds = oppfolgingFeedRepository.hentAlleBrukereUnderOppfolging();

            log.info("Publiserer {} brukere på kafka", aktorIds.size());
            val job = runAsyncJob(() -> {
                        val count = aktorIds.stream()
                                .map(oppfolgingKafkaProducer::send)
                                .filter(Try::isSuccess)
                                .count();

                        log.info("Fullført! Sendte {} meldinger på kafka", count);
                    }
            );

            val mld = String.format("Startet jobb med id %s på pod %s", job.getJobId(), job.getPodName());
            resp.setStatus(SC_OK);
            resp.getWriter().write(mld);

        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
