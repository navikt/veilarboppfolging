package no.nav.veilarboppfolging.controller;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.utils.Credentials;
import no.nav.common.utils.job.JobUtils;
import no.nav.common.utils.job.RunningJob;
import no.nav.veilarboppfolging.client.veilarbportefolje.OppfolgingEnhetPageDTO;
import no.nav.veilarboppfolging.client.veilarbportefolje.VeilarbportefoljeClient;
import no.nav.veilarboppfolging.repository.OppfolgingsenhetHistorikkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static java.util.Optional.ofNullable;
import static no.nav.veilarboppfolging.utils.AuthorizationUtils.isBasicAuthAuthorized;

@Slf4j
@RestController
@RequestMapping("/internal/populer_enhet_historikk")
public class PopulerOppfolgingHistorikkController {

    private static final Integer MAX_PAGE_NUMBER = 1000;

    private final OppfolgingsenhetHistorikkRepository repository;

    private final VeilarbportefoljeClient veilarbportefoljeClient;

    private final Credentials serviceUserCredentials;

    @Autowired
    public PopulerOppfolgingHistorikkController(OppfolgingsenhetHistorikkRepository repository, VeilarbportefoljeClient veilarbportefoljeClient, Credentials serviceUserCredentials) {
        this.repository = repository;
        this.veilarbportefoljeClient = veilarbportefoljeClient;
        this.serviceUserCredentials = serviceUserCredentials;
    }

    @GetMapping
    public ResponseEntity populerOppfolgingHistorikk(HttpServletRequest req) {
        int pageNumber = ofNullable(req.getParameter("page_number")).map(Integer::parseInt).orElse(1);
        int pageSize = ofNullable(req.getParameter("page_size")).map(Integer::parseInt).orElse(100);

        if (!isBasicAuthAuthorized(req, serviceUserCredentials.username, serviceUserCredentials.password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        RunningJob job = JobUtils.runAsyncJob(() -> fetchPages(pageNumber, pageSize));

        String message = String.format("Startet populering av enhetshistorikk med jobId %s på pod %s  (page_number: %s page_size: %s", job.getJobId(), job.getPodName(), pageNumber, pageSize);

        return ResponseEntity.ok(message);
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
