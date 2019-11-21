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

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static no.nav.internal.AuthorizationUtils.isBasicAuthAuthorized;

@Slf4j
public class PopulerOppfolgingHistorikkServlet extends HttpServlet {

    private static final Integer MAX_PAGE_NUMBER = 3500;
    private static final int PAGE_SIZE = 1000;

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
        if (isBasicAuthAuthorized(req)) {
            RunningJob job = JobUtils.runAsyncJob(this::fetchAllPages);
            resp.getWriter().write(String.format("Startet populering av enhetshistorikk med jobId %s på pod %s", job.getJobId(), job.getPodName()));
            resp.setStatus(SC_OK);
        } else {
            AuthorizationUtils.writeUnauthorized(resp);
        }
    }

    private void fetchAllPages() {
        Integer nextPage = 1;

        while (nextPage != null && nextPage < MAX_PAGE_NUMBER) {

            OppfolgingEnhetPageDTO page = fetchPage(nextPage);

            log.info("Inserting {} elements from page {} into database", page.getUsers().size(), page.getPage_number());
            page.getUsers().forEach(repository::insertOppfolgingsenhetEndring);

            nextPage = page.getPage_next();
        }
    }

    private OppfolgingEnhetPageDTO fetchPage(int pageNumber) {

        log.info("Fetching page {}", pageNumber);

        return RestUtils.withClient(client -> client.target(VEILARBPORTEFOLJE_API_URL)
                .queryParam("page_number", pageNumber)
                .queryParam("page_size", PAGE_SIZE)
                .request(APPLICATION_JSON_TYPE)
                .header(AUTHORIZATION, "Bearer " + systemUserTokenProvider.getToken())
                .header("Nav-Call-Id", IdUtils.generateId())
                .header("Nav-Consumer-Id", EnvironmentUtils.getApplicationName())
                .get(OppfolgingEnhetPageDTO.class));
    }
}
