package no.nav.veilarboppfolging.internal;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.veilarboppfolging.domain.AktorId;
import no.nav.veilarboppfolging.kafka.OppfolgingStatusKafkaProducer;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static no.nav.veilarboppfolging.internal.AuthorizationUtils.isBasicAuthAuthorized;

public class PubliserOppfolgingStatusServlet extends HttpServlet {

    private final OppfolgingStatusKafkaProducer oppfolgingStatusKafkaProducer;

    @Inject
    public PubliserOppfolgingStatusServlet(OppfolgingStatusKafkaProducer oppfolgingStatusKafkaProducer) {
        this.oppfolgingStatusKafkaProducer = oppfolgingStatusKafkaProducer;
    }

    @Override
    @SneakyThrows
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        if (isBasicAuthAuthorized(req)) {
            String aktoerId = req.getParameter("aktoerId");
            if (aktoerId == null) {
                resp.getWriter().write("Ingen gyldig aktoerId oppgitt: /internal/publiser_oppfolging?aktoerId=<aktoerId>");
                resp.setStatus(SC_BAD_REQUEST);
                return;
            }
            oppfolgingStatusKafkaProducer.send(new AktorId(aktoerId));
            val mld = String.format("Sendte melding på kafka for bruker %s", aktoerId);
            resp.setStatus(SC_OK);
            resp.getWriter().write(mld);
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
