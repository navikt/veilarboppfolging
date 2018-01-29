package no.nav.fo.veilarboppfolging.services;

import no.nav.fo.veilarboppfolging.db.KvpRepository;
import no.nav.fo.veilarboppfolging.domain.Kvp;
import no.nav.fo.veilarboppfolging.domain.MalData;
import no.nav.fo.veilarboppfolging.services.OppfolgingResolver.OppfolgingResolverDependencies;
import no.nav.fo.veilarboppfolging.utils.KvpUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Component
public class MalService {

    @Inject
    private OppfolgingResolverDependencies oppfolgingResolverDependencies;

    @Inject
    private EnhetPepClient enhetPepClient;

    @Inject
    private KvpRepository kvpRepository;

    public MalData hentMal(String fnr) {
        OppfolgingResolver resolver = new OppfolgingResolver(fnr, oppfolgingResolverDependencies);
        MalData gjeldendeMal = resolver.getOppfolging().getGjeldendeMal();

        List<Kvp> kvpList = kvpRepository.hentKvpHistorikk(resolver.getAktorId());
        if (!KvpUtils.sjekkTilgangGittKvp(enhetPepClient, kvpList, gjeldendeMal::getDato)) {
            return new MalData();
        }
        return Optional.ofNullable(gjeldendeMal).orElse(new MalData());
    }

    public List<MalData> hentMalList(String fnr) {
        OppfolgingResolver resolver = new OppfolgingResolver(fnr, oppfolgingResolverDependencies);
        List<MalData> malList = resolver.getMalList();

        List<Kvp> kvpList = kvpRepository.hentKvpHistorikk(resolver.getAktorId());
        return malList.stream().filter(mal -> KvpUtils.sjekkTilgangGittKvp(enhetPepClient, kvpList, mal::getDato)).collect(toList());
    }

    public MalData oppdaterMal(String mal, String fnr, String endretAv) {
        return new OppfolgingResolver(fnr, oppfolgingResolverDependencies).oppdaterMal(mal, endretAv);
    }

    public void slettMal(String fnr) {
        new OppfolgingResolver(fnr, oppfolgingResolverDependencies).slettMal();
    }

}
