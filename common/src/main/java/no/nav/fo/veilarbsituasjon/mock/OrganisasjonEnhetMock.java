package no.nav.fo.veilarbsituasjon.mock;

import no.nav.tjeneste.virksomhet.organisasjonenhet.v1.*;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v1.meldinger.*;

public class OrganisasjonEnhetMock implements OrganisasjonEnhetV1 {
    @Override
    public WSFinnArbeidsfordelingForEnhetBolkResponse finnArbeidsfordelingForEnhetBolk(WSFinnArbeidsfordelingForEnhetBolkRequest wsFinnArbeidsfordelingForEnhetBolkRequest) throws FinnArbeidsfordelingForEnhetBolkUgyldigInput {
        return null;
    }

    @Override
    public WSFinnNAVKontorForGeografiskNedslagsfeltBolkResponse finnNAVKontorForGeografiskNedslagsfeltBolk(WSFinnNAVKontorForGeografiskNedslagsfeltBolkRequest wsFinnNAVKontorForGeografiskNedslagsfeltBolkRequest) throws FinnNAVKontorForGeografiskNedslagsfeltBolkUgyldigInput {
        return null;
    }

    @Override
    public WSFinnArbeidsfordelingBolkResponse finnArbeidsfordelingBolk(WSFinnArbeidsfordelingBolkRequest wsFinnArbeidsfordelingBolkRequest) throws FinnArbeidsfordelingBolkUgyldigInput {
        return null;
    }

    @Override
    public WSFinnEnheterForArbeidsfordelingBolkResponse finnEnheterForArbeidsfordelingBolk(WSFinnEnheterForArbeidsfordelingBolkRequest wsFinnEnheterForArbeidsfordelingBolkRequest) throws FinnEnheterForArbeidsfordelingBolkUgyldigInput {
        return null;
    }

    @Override
    public WSHentEnhetBolkResponse hentEnhetBolk(WSHentEnhetBolkRequest wsHentEnhetBolkRequest) throws HentEnhetBolkUgyldigInput {
        return null;
    }

    @Override
    public WSHentFullstendigEnhetListeResponse hentFullstendigEnhetListe(WSHentFullstendigEnhetListeRequest wsHentFullstendigEnhetListeRequest) {
        return null;
    }

    @Override
    public void ping() {

    }
}
