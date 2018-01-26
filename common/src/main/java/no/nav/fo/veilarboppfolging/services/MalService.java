package no.nav.fo.veilarboppfolging.services;

import no.nav.fo.veilarboppfolging.domain.MalData;
import no.nav.fo.veilarboppfolging.services.OppfolgingResolver.OppfolgingResolverDependencies;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

@Component
public class MalService {

    @Inject
    private OppfolgingResolverDependencies oppfolgingResolverDependencies;

    public MalData hentMal(String fnr) {
        MalData gjeldendeMal = new OppfolgingResolver(fnr, oppfolgingResolverDependencies).getOppfolging().getGjeldendeMal();
        return Optional.ofNullable(gjeldendeMal).orElse(new MalData());
    }

    public List<MalData> hentMalList(String fnr) {
        return new OppfolgingResolver(fnr, oppfolgingResolverDependencies).getMalList();
    }

    public MalData oppdaterMal(String mal, String fnr, String endretAv) {
        return new OppfolgingResolver(fnr, oppfolgingResolverDependencies).oppdaterMal(mal, endretAv);
    }

    public void slettMal(String fnr) {
        new OppfolgingResolver(fnr, oppfolgingResolverDependencies).slettMal();
    }

}
