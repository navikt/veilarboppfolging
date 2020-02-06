package no.nav.fo.veilarboppfolging.services;

import lombok.SneakyThrows;
import no.nav.apiapp.feil.Feil;
import no.nav.apiapp.security.PepClient;
import no.nav.fo.veilarboppfolging.db.KvpRepository;
import no.nav.fo.veilarboppfolging.domain.Kvp;
import no.nav.fo.veilarboppfolging.domain.MalData;
import no.nav.fo.veilarboppfolging.rest.AutorisasjonService;
import no.nav.fo.veilarboppfolging.services.OppfolgingResolver.OppfolgingResolverDependencies;
import no.nav.fo.veilarboppfolging.utils.FunksjonelleMetrikker;
import no.nav.fo.veilarboppfolging.utils.KvpUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static no.nav.apiapp.feil.FeilType.INGEN_TILGANG;

@Component
public class MalService {

    @Inject
    private OppfolgingResolverDependencies oppfolgingResolverDependencies;

    @Inject
    private PepClient pepClient;

    @Inject
    private KvpRepository kvpRepository;

    @Inject
    AutorisasjonService autorisasjonService;

    public MalData hentMal(String fnr) {
        autorisasjonService.sjekkLesetilgangTilBruker(fnr);

        OppfolgingResolver resolver = OppfolgingResolver.lagOppfolgingResolver(fnr, oppfolgingResolverDependencies);
        MalData gjeldendeMal = resolver.getOppfolging().getGjeldendeMal();

        if (gjeldendeMal == null) {
            return new MalData();
        }

        List<Kvp> kvpList = kvpRepository.hentKvpHistorikk(resolver.getAktorId());
        if (!KvpUtils.sjekkTilgangGittKvp(pepClient, kvpList, gjeldendeMal::getDato)) {
            return new MalData();
        }
        return gjeldendeMal;
    }

    public List<MalData> hentMalList(String fnr) {
        autorisasjonService.sjekkLesetilgangTilBruker(fnr);

        OppfolgingResolver resolver = OppfolgingResolver.lagOppfolgingResolver(fnr, oppfolgingResolverDependencies);
        List<MalData> malList = resolver.getMalList();

        List<Kvp> kvpList = kvpRepository.hentKvpHistorikk(resolver.getAktorId());
        return malList.stream().filter(mal -> KvpUtils.sjekkTilgangGittKvp(pepClient, kvpList, mal::getDato)).collect(toList());
    }

    public MalData oppdaterMal(String mal, String fnr, String endretAvVeileder) {
        autorisasjonService.sjekkLesetilgangTilBruker(fnr);

        OppfolgingResolver resolver = OppfolgingResolver.lagOppfolgingResolver(fnr, oppfolgingResolverDependencies);

        Kvp kvp = kvpRepository.fetch(kvpRepository.gjeldendeKvp(resolver.getAktorId()));
        ofNullable(kvp).ifPresent(this::sjekkEnhetTilgang);

        MalData malData = resolver.oppdaterMal(mal, endretAvVeileder);
        FunksjonelleMetrikker.oppdatertMittMal(malData, resolver.getMalList().size());
        return malData;
    }



    @SneakyThrows
    private void sjekkEnhetTilgang(Kvp kvp) {
        if(!pepClient.harTilgangTilEnhet(kvp.getEnhet())) {
            throw new Feil(INGEN_TILGANG);
        }
    }

}
