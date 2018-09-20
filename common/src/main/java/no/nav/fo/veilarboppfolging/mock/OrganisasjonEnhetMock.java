package no.nav.fo.veilarboppfolging.mock;

import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.binding.FinnNAVKontorUgyldigInput;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.binding.HentOverordnetEnhetListeEnhetIkkeFunnet;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.binding.OrganisasjonEnhetV2;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v2.meldinger.*;

public class OrganisasjonEnhetMock implements OrganisasjonEnhetV2 {

    @Override
    public FinnNAVKontorResponse finnNAVKontor(FinnNAVKontorRequest finnNAVKontorRequest) throws FinnNAVKontorUgyldigInput {
        return null;
    }

    @Override
    public HentEnhetBolkResponse hentEnhetBolk(HentEnhetBolkRequest hentEnhetBolkRequest) {
        return null;
    }

    @Override
    public HentFullstendigEnhetListeResponse hentFullstendigEnhetListe(HentFullstendigEnhetListeRequest hentFullstendigEnhetListeRequest) {
        return null;
    }

    @Override
    public void ping() {

    }

    @Override
    public HentOverordnetEnhetListeResponse hentOverordnetEnhetListe(HentOverordnetEnhetListeRequest hentOverordnetEnhetListeRequest) throws HentOverordnetEnhetListeEnhetIkkeFunnet {
        return null;
    }
}
