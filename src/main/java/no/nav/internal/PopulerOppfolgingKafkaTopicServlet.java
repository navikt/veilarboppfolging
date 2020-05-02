package no.nav.internal;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.common.utils.IdUtils;
import no.nav.fo.veilarboppfolging.db.OppfolgingFeedRepository;
import no.nav.fo.veilarboppfolging.domain.AktorId;
import no.nav.fo.veilarboppfolging.kafka.OppfolgingKafkaProducer;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingKafkaDTO;
import org.slf4j.MDC;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static no.nav.internal.AuthorizationUtils.isBasicAuthAuthorized;

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

            String jobId = IdUtils.generateId();
            MDC.put("jobId", jobId);

            Supplier<Long> supplier = () -> aktorIds.stream()
                    .map(AktorId::getAktorId)
                    .map(aktorId -> OppfolgingKafkaDTO.builder().aktoerid(aktorId).build())
                    .map(oppfolgingKafkaProducer::send)
                    .count();

            val future = CompletableFuture.supplyAsync(supplier);

            while (!future.isDone()) {
                //busy wait
            }

            resp.setStatus(SC_OK);
            String ferdig = String.format("Startet populering av kafka med jobId: %s", jobId);
            resp.getWriter().write(ferdig);
            MDC.remove("jobId");

        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
