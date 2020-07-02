package no.nav.veilarboppfolging.internal;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.common.utils.Credentials;
import no.nav.veilarboppfolging.domain.AktorId;
import no.nav.veilarboppfolging.kafka.OppfolgingStatusKafkaProducer;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static no.nav.veilarboppfolging.internal.AuthorizationUtils.isBasicAuthAuthorized;

@WebServlet(
        name = "PubliserOppfolgingStatusServlet",
        urlPatterns = {"/internal/publiser_oppfolging_status"}
)
public class PubliserOppfolgingStatusServlet extends HttpServlet {

    private final OppfolgingStatusKafkaProducer oppfolgingStatusKafkaProducer;

    private final Credentials serviceUserCredentials;

    @Autowired
    public PubliserOppfolgingStatusServlet(OppfolgingStatusKafkaProducer oppfolgingStatusKafkaProducer, Credentials serviceUserCredentials) {
        this.oppfolgingStatusKafkaProducer = oppfolgingStatusKafkaProducer;
        this.serviceUserCredentials = serviceUserCredentials;
    }

    @Override
    @SneakyThrows
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        if (isBasicAuthAuthorized(req, serviceUserCredentials.username, serviceUserCredentials.password)) {
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
