package no.nav.veilarboppfolging.service;

import lombok.RequiredArgsConstructor;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.repository.EskaleringsvarselRepository;
import no.nav.veilarboppfolging.repository.OppfolgingsStatusRepository;
import no.nav.veilarboppfolging.repository.entity.EskaleringsvarselEntity;
import no.nav.veilarboppfolging.repository.entity.OppfolgingEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EskaleringService {

    private final ArenaOppfolgingService arenaOppfolgingService;
    private final AuthService authService;
    private final UnleashService unleashService;
    private final OppfolgingsStatusRepository oppfolgingsStatusRepository;
    private final EskaleringsvarselRepository eskaleringsvarselRepository;


    public void stoppEskalering(Fnr fnr, String veilederId, String begrunnelse) {

        if(unleashService.skrudAvStoppEskalering()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "stop eskaleringsvarsel flyttes til veilarbdialog");
        }

        AktorId aktorId = authService.getAktorIdOrThrow(fnr);
        String oppfolgingsEnhet = hentOppfolgingsEnhet(fnr);

        authService.sjekkLesetilgangMedAktorId(aktorId);
        authService.sjekkTilgangTilEnhet(oppfolgingsEnhet);

        long gjeldendeEskaleringsvarselId = hentGjeldendeEskaleringsvarselId(aktorId);

        if (gjeldendeEskaleringsvarselId == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Brukeren har ikke et aktivt eskaleringsvarsel");
        }

        eskaleringsvarselRepository.finish(aktorId, gjeldendeEskaleringsvarselId, veilederId, begrunnelse, ZonedDateTime.now());
    }

    public void stoppEskaleringForAvsluttOppfolging(AktorId aktorId, String veilederId, String begrunnelse) {
        long gjeldendeEskaleringsvarselId = hentGjeldendeEskaleringsvarselId(aktorId);

        if (gjeldendeEskaleringsvarselId != 0) {
            eskaleringsvarselRepository.finish(aktorId, gjeldendeEskaleringsvarselId, veilederId, begrunnelse, ZonedDateTime.now());
        }
    }

    public Optional<EskaleringsvarselEntity> hentGjeldendeEskaleringsvarsel(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);
        return hentGjeldendeEskaleringsvarsel(aktorId);
    }

    private Optional<EskaleringsvarselEntity> hentGjeldendeEskaleringsvarsel(AktorId aktorId) {
        return eskaleringsvarselRepository.hentGjeldendeEskaleringsvarsel(aktorId);
    }

    private long hentGjeldendeEskaleringsvarselId(AktorId aktorId) {
        return oppfolgingsStatusRepository.hentOppfolging(aktorId)
                .map(OppfolgingEntity::getGjeldendeEskaleringsvarselId)
                .orElse(0L);
    }

    private String hentOppfolgingsEnhet(Fnr fnr) {
        return arenaOppfolgingService.hentOppfolgingFraVeilarbarena(fnr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)).getNav_kontor();
    }

}
