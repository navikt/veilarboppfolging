package no.nav.internal;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.brukerdialog.security.oidc.SystemUserTokenProvider;
import no.nav.common.utils.IdUtils;
import no.nav.fo.veilarboppfolging.db.OppfolgingsenhetHistorikkRepository;
import no.nav.jobutils.JobUtils;
import no.nav.jobutils.RunningJob;
import no.nav.sbl.rest.RestUtils;
import no.nav.sbl.util.EnvironmentUtils;

import javax.inject.Inject;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Optional;

import static java.lang.Integer.parseInt;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static no.nav.fo.veilarboppfolging.db.OppfolgingsenhetHistorikkRepository.TABLENAME;
import static no.nav.internal.AuthorizationUtils.isBasicAuthAuthorized;

@Slf4j
public class PopulerOppfolgingHistorikkServlet extends HttpServlet {

    private static final Integer MAX_PAGE_NUMBER = 1000;

    private static final String VEILARBPORTEFOLJE_API_URL = EnvironmentUtils.getRequiredProperty("VEILARBPORTEFOLJEAPI_URL");

    private OppfolgingsenhetHistorikkRepository repository;
    private SystemUserTokenProvider systemUserTokenProvider;

    @Inject
    public PopulerOppfolgingHistorikkServlet(OppfolgingsenhetHistorikkRepository repository, SystemUserTokenProvider systemUserTokenProvider) {
        this.repository = repository;
        this.systemUserTokenProvider = systemUserTokenProvider;
    }

    @Override
    @SneakyThrows
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {

        int pageNumber = Optional.of(parseInt(req.getParameter("page_number"))).orElse(1);
        int pageSize = Optional.of(parseInt(req.getParameter("page_size"))).orElse(100);

        if (isBasicAuthAuthorized(req)) {
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

        log.info("Truncating table {}", TABLENAME);
        repository.truncateOppfolgingsenhetEndret();

        do {
            OppfolgingEnhetPageDTO page = fetchPage(pageNumber, pageSize);

            log.info("Inserting {} elements from page {} into database", page.getUsers().size(), page.getPage_number());
            repository.insertOppfolgingsenhetEndring(page.getUsers());

            if (totalNumberOfPages == null) {
                totalNumberOfPages = page.getPage_number_total();
            }

            pageNumber++;

        } while (pageNumber <= totalNumberOfPages || pageNumber < MAX_PAGE_NUMBER);

        log.info("Finished fetching {} pages", totalNumberOfPages);
    }

    private OppfolgingEnhetPageDTO fetchPage(int pageNumber, int pageSize) {

        log.info("Fetching page {}", pageNumber);

        return RestUtils.withClient(client -> client.target(VEILARBPORTEFOLJE_API_URL + "/oppfolgingenhet")
                .queryParam("page_number", pageNumber)
                .queryParam("page_size", pageSize)
                .request(APPLICATION_JSON_TYPE)
                .header(AUTHORIZATION, "Bearer " + systemUserTokenProvider.getToken())
                .header("Nav-Call-Id", IdUtils.generateId())
                .header("Nav-Consumer-Id", EnvironmentUtils.getApplicationName())
                .get(OppfolgingEnhetPageDTO.class));
    }
}
