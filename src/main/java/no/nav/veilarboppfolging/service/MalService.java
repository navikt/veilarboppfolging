package no.nav.veilarboppfolging.service;

import no.nav.veilarboppfolging.domain.Kvp;
import no.nav.veilarboppfolging.domain.MalData;
import no.nav.veilarboppfolging.repository.KvpRepository;
import no.nav.veilarboppfolging.service.OppfolgingResolver.OppfolgingResolverDependencies;
import no.nav.veilarboppfolging.utils.KvpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

@Service
public class MalService {

    private final MetricsService metricsService;

    private final OppfolgingResolverDependencies oppfolgingResolverDependencies;

    private final KvpRepository kvpRepository;

    private final AuthService authService;

    @Autowired
    public MalService(
            MetricsService metricsService,
            OppfolgingResolverDependencies oppfolgingResolverDependencies,
            KvpRepository kvpRepository,
            AuthService authService
    ) {
        this.metricsService = metricsService;
        this.oppfolgingResolverDependencies = oppfolgingResolverDependencies;
        this.kvpRepository = kvpRepository;
        this.authService = authService;
    }

    public MalData hentMal(String fnr) {
        authService.sjekkLesetilgangMedFnr(fnr);

        OppfolgingResolver resolver = OppfolgingResolver.lagOppfolgingResolver(fnr, oppfolgingResolverDependencies);
        MalData gjeldendeMal = resolver.getOppfolging().getGjeldendeMal();

        if (gjeldendeMal == null) {
            return new MalData();
        }

        List<Kvp> kvpList = kvpRepository.hentKvpHistorikk(resolver.getAktorId());
        if (!KvpUtils.sjekkTilgangGittKvp(authService, kvpList, gjeldendeMal::getDato)) {
            return new MalData();
        }

        return gjeldendeMal;
    }

    public List<MalData> hentMalList(String fnr) {
        authService.sjekkLesetilgangMedFnr(fnr);

        OppfolgingResolver resolver = OppfolgingResolver.lagOppfolgingResolver(fnr, oppfolgingResolverDependencies);
        List<MalData> malList = resolver.getMalList();

        List<Kvp> kvpList = kvpRepository.hentKvpHistorikk(resolver.getAktorId());
        return malList.stream().filter(mal -> KvpUtils.sjekkTilgangGittKvp(authService, kvpList, mal::getDato)).collect(toList());
    }

    public MalData oppdaterMal(String mal, String fnr, String endretAvVeileder) {
        authService.sjekkLesetilgangMedFnr(fnr);

        OppfolgingResolver resolver = OppfolgingResolver.lagOppfolgingResolver(fnr, oppfolgingResolverDependencies);

        Kvp kvp = kvpRepository.fetch(kvpRepository.gjeldendeKvp(resolver.getAktorId()));
        ofNullable(kvp).ifPresent(this::sjekkEnhetTilgang);

        MalData malData = resolver.oppdaterMal(mal, endretAvVeileder);
        metricsService.oppdatertMittMal(malData, resolver.getMalList().size());
        return malData;
    }

    private void sjekkEnhetTilgang(Kvp kvp) {
        if (!authService.harTilgangTilEnhet(kvp.getEnhet())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

}
