package no.nav.internal;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarboppfolging.kafka.OppfolgingKafkaProducer;
import no.nav.jobutils.JobUtils;
import no.nav.jobutils.RunningJob;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static no.nav.internal.AuthorizationUtils.isBasicAuthAuthorized;

@Slf4j
public class PopulerOppfolgingKafkaTopicServlet extends HttpServlet {

    private final OppfolgingKafkaProducer kafka;

    @Inject
    public PopulerOppfolgingKafkaTopicServlet(OppfolgingKafkaProducer kafka) {
        this.kafka = kafka;
    }

    @Override
    @SneakyThrows
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        if (isBasicAuthAuthorized(req)) {
            RunningJob job = JobUtils.runAsyncJob(kafka::publiserAlleBrukere);
            resp.setStatus(SC_OK);
            String ferdig = String.format("Startet populering av kafka med jobId: %s på pod: %s", job.getJobId(), job.getPodName());
            resp.getWriter().write(ferdig);
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
