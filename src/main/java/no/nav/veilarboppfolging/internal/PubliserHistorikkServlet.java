package no.nav.veilarboppfolging.internal;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.utils.Credentials;
import no.nav.common.utils.job.JobUtils;
import no.nav.common.utils.job.RunningJob;
import no.nav.veilarboppfolging.kafka.OppfolgingStatusKafkaProducer;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static no.nav.veilarboppfolging.internal.AuthorizationUtils.isBasicAuthAuthorized;

@Slf4j
@WebServlet(
        name = "PubliserHistorikkServlet",
        urlPatterns = {"/internal/publiser_oppfolging_status_historikk"}
)
public class PubliserHistorikkServlet extends HttpServlet {

    private final OppfolgingStatusKafkaProducer kafka;

    private final Credentials serviceUserCredentials;

    @Autowired
    public PubliserHistorikkServlet(OppfolgingStatusKafkaProducer kafka, Credentials serviceUserCredentials) {
        this.kafka = kafka;
        this.serviceUserCredentials = serviceUserCredentials;
    }

    @Override
    @SneakyThrows
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        if (isBasicAuthAuthorized(req, serviceUserCredentials.username, serviceUserCredentials.password)) {
            RunningJob job = JobUtils.runAsyncJob(kafka::publiserAlleBrukere);
            resp.setStatus(SC_OK);
            String ferdig = String.format("Startet populering av kafka med jobId: %s på pod: %s", job.getJobId(), job.getPodName());
            resp.getWriter().write(ferdig);
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
