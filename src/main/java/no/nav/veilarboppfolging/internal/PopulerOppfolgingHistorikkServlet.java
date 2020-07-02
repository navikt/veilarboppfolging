package no.nav.veilarboppfolging.internal;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import no.nav.common.utils.Credentials;
import no.nav.common.utils.job.JobUtils;
import no.nav.common.utils.job.RunningJob;
import no.nav.veilarboppfolging.client.veilarbportefolje.OppfolgingEnhetPageDTO;
import no.nav.veilarboppfolging.client.veilarbportefolje.VeilarbportefoljeClient;
import no.nav.veilarboppfolging.db.OppfolgingsenhetHistorikkRepository;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Optional;

import static java.lang.Integer.parseInt;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static no.nav.veilarboppfolging.internal.AuthorizationUtils.isBasicAuthAuthorized;

@Slf4j
@WebServlet(
        name = "PopulerOppfolgingHistorikk",
        urlPatterns = {"/internal/populer_enhet_historikk"}
)
public class PopulerOppfolgingHistorikkServlet extends HttpServlet {

    private static final Integer MAX_PAGE_NUMBER = 1000;

    private final OppfolgingsenhetHistorikkRepository repository;

    private final VeilarbportefoljeClient veilarbportefoljeClient;

    private final Credentials serviceUserCredentials;

    @Autowired
    public PopulerOppfolgingHistorikkServlet(OppfolgingsenhetHistorikkRepository repository, VeilarbportefoljeClient veilarbportefoljeClient, Credentials serviceUserCredentials) {
        this.repository = repository;
        this.veilarbportefoljeClient = veilarbportefoljeClient;
        this.serviceUserCredentials = serviceUserCredentials;
    }

    @Override
    @SneakyThrows
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {

        int pageNumber = Optional.of(parseInt(req.getParameter("page_number"))).orElse(1);
        int pageSize = Optional.of(parseInt(req.getParameter("page_size"))).orElse(100);

        if (isBasicAuthAuthorized(req, serviceUserCredentials.username, serviceUserCredentials.password)) {
            RunningJob job = JobUtils.runAsyncJob(() -> fetchPages(pageNumber, pageSize));
            resp.getWriter().write(String.format("Startet populering av enhetshistorikk med jobId %s på pod %s  (page_number: %s page_size: %s", job.getJobId(), job.getPodName(), pageNumber, pageSize));
            resp.setStatus(SC_OK);
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }

    @SneakyThrows
    private void fetchPages(int pageNumber, int pageSize) {

        Integer totalNumberOfPages = null;

        log.info("Truncating table OPPFOLGINGSENHET_ENDRET");
        repository.truncateOppfolgingsenhetEndret();

        do {
            log.info("Fetching page {}", pageNumber);

            OppfolgingEnhetPageDTO page = veilarbportefoljeClient.hentEnhetPage(pageNumber, pageSize);

            log.info("Inserting {} elements from page {} into database", page.getUsers().size(), page.getPage_number());
            repository.insertOppfolgingsenhetEndring(page.getUsers());

            if (totalNumberOfPages == null) {
                totalNumberOfPages = page.getPage_number_total();
            }

            pageNumber++;

        } while (pageNumber <= totalNumberOfPages || pageNumber < MAX_PAGE_NUMBER);

        log.info("Finished fetching {} pages", totalNumberOfPages);
    }

}
