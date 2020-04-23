package no.nav.internal;

import lombok.SneakyThrows;
import no.nav.fo.veilarboppfolging.domain.AktorId;
import no.nav.fo.veilarboppfolging.kafka.OppfolgingKafkaProducer;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static no.nav.internal.AuthorizationUtils.isBasicAuthAuthorized;

public class PubliserOppfolgingKafkaTopicServlet extends HttpServlet {

    private final OppfolgingKafkaProducer oppfolgingKafkaProducer;

    @Inject
    public PubliserOppfolgingKafkaTopicServlet(OppfolgingKafkaProducer oppfolgingKafkaProducer) {
        this.oppfolgingKafkaProducer = oppfolgingKafkaProducer;
    }

    @Override
    @SneakyThrows
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        if (isBasicAuthAuthorized(req)) {
            String aktoerId = req.getParameter("aktoerId");
            if (aktoerId == null) {
                resp.setStatus(SC_BAD_REQUEST);
                return;
            }
            oppfolgingKafkaProducer.sendAsync(new AktorId(aktoerId));
            resp.setStatus(SC_OK);
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
