package no.nav.internal;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarboppfolging.db.OppfolgingFeedRepository;
import no.nav.fo.veilarboppfolging.domain.AktorId;
import no.nav.fo.veilarboppfolging.kafka.OppfolgingKafkaProducer;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

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
            aktorIds.forEach(oppfolgingKafkaProducer::sendAsync);
            resp.setStatus(SC_OK);
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }
}
