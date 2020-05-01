package no.nav.internal;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.fo.veilarboppfolging.db.OppfolgingFeedRepository;
import no.nav.fo.veilarboppfolging.domain.AktorId;
import no.nav.fo.veilarboppfolging.kafka.OppfolgingKafkaProducer;
import no.nav.fo.veilarboppfolging.rest.domain.OppfolgingKafkaDTO;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

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
                                                          .map(AktorId::getAktorId)
                                                          .map(aktorId -> OppfolgingKafkaDTO.builder().aktoerid(aktorId).build())
                                                          .map(oppfolgingKafkaProducer::send)
                                                          .count();

                                      log.info("Fullført! Sendte {} asynce meldinger på kafka", count);
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
