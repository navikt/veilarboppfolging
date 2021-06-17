package no.nav.veilarboppfolging.service;

import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.client.varseloppgave.VarseloppgaveClient;
import no.nav.veilarboppfolging.domain.EskaleringsvarselData;
import no.nav.veilarboppfolging.repository.EskaleringsvarselRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EskaleringService {

    private final ArenaOppfolgingService arenaOppfolgingService;

    private final AuthService authService;

    private final TransactionTemplate transactor;

    private final VarseloppgaveClient varseloppgaveClient;

    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;

    private final EskaleringsvarselRepository eskaleringsvarselRepository;

    @Autowired
    public EskaleringService(
            ArenaOppfolgingService arenaOppfolgingService,
            AuthService authService,
            TransactionTemplate transactor,
            VarseloppgaveClient varseloppgaveClient,
            OppfolgingsStatusRepository oppfolgingsStatusRepository,
            EskaleringsvarselRepository eskaleringsvarselRepository
    ) {
        this.arenaOppfolgingService = arenaOppfolgingService;
        this.authService = authService;
        this.transactor = transactor;
        this.varseloppgaveClient = varseloppgaveClient;
        this.oppfolgingsStatusRepository = oppfolgingsStatusRepository;
        this.eskaleringsvarselRepository = eskaleringsvarselRepository;
    }

    public void startEskalering(Fnr fnr, String veilederId, String begrunnelse, long tilhorendeDialogId) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);
        String oppfolgingsEnhet = hentOppfolgingsEnhet(fnr);

        authService.sjekkLesetilgangMedAktorId(aktorId);
        authService.sjekkTilgangTilEnhet(oppfolgingsEnhet);

        transactor.executeWithoutResult((status) -> {
            long gjeldendeEskaleringsvarselId = oppfolgingsStatusRepository.fetch(aktorId).getGjeldendeEskaleringsvarselId();

            if (gjeldendeEskaleringsvarselId > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brukeren har allerede et aktivt eskaleringsvarsel.");
            }

            EskaleringsvarselData eskaleringsvarselData = EskaleringsvarselData.builder()
                    .aktorId(aktorId.get())
                    .opprettetAv(veilederId)
                    .opprettetBegrunnelse(begrunnelse)
                    .tilhorendeDialogId(tilhorendeDialogId)
                    .build();

            eskaleringsvarselRepository.create(eskaleringsvarselData);

            varseloppgaveClient.sendEskaleringsvarsel(aktorId, tilhorendeDialogId);
        });
    }

    public void stoppEskalering(Fnr fnr, String veilederId, String begrunnelse) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);
        String oppfolgingsEnhet = hentOppfolgingsEnhet(fnr);

        authService.sjekkLesetilgangMedAktorId(aktorId);
        authService.sjekkTilgangTilEnhet(oppfolgingsEnhet);

        long gjeldendeEskaleringsvarselId = oppfolgingsStatusRepository.fetch(aktorId).getGjeldendeEskaleringsvarselId();

        if (gjeldendeEskaleringsvarselId == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brukeren har ikke et aktivt eskaleringsvarsel");
        }

        eskaleringsvarselRepository.finish(aktorId, gjeldendeEskaleringsvarselId, veilederId, begrunnelse);
    }

    public void stoppEskaleringForAvsluttOppfolging(AktorId aktorId, String veilederId, String begrunnelse) {
        long gjeldendeEskaleringsvarselId = oppfolgingsStatusRepository.fetch(aktorId).getGjeldendeEskaleringsvarselId();

        if (gjeldendeEskaleringsvarselId != 0) {
            eskaleringsvarselRepository.finish(aktorId, gjeldendeEskaleringsvarselId, veilederId, begrunnelse);
        }
    }

    private String hentOppfolgingsEnhet(Fnr fnr) {
        return arenaOppfolgingService.hentOppfolgingFraVeilarbarena(fnr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)).getNav_kontor();
    }

}
