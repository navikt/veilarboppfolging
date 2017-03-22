package no.nav.fo.veilarbsituasjon.services;

import no.nav.fo.veilarbsituasjon.rest.domain.Organisasjonsenhet;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v1.HentEnhetBolkUgyldigInput;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v1.OrganisasjonEnhetV1;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v1.informasjon.WSDetaljertEnhet;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v1.meldinger.WSHentEnhetBolkRequest;
import no.nav.tjeneste.virksomhet.organisasjonenhet.v1.meldinger.WSHentEnhetBolkResponse;

import java.util.List;
import java.util.NoSuchElementException;

public class OrganisasjonsenhetService {

    private final OrganisasjonEnhetV1 organisasjonenhetWs;

    public OrganisasjonsenhetService(OrganisasjonEnhetV1 organisasjonEnhetV1) {
        this.organisasjonenhetWs = organisasjonEnhetV1;
    }

    public Organisasjonsenhet hentEnhet(String enhetId) {
        Organisasjonsenhet organisasjonsenhet = new Organisasjonsenhet().withEnhetId(enhetId);
        try {
            WSHentEnhetBolkResponse response = organisasjonenhetWs.hentEnhetBolk(new WSHentEnhetBolkRequest().withEnhetIdListe(enhetId));
            return organisasjonsenhet.withNavn(getNavn(response.getEnhetListe(), enhetId));
        } catch (HentEnhetBolkUgyldigInput hentEnhetBolkUgyldigInput) {
            throw new IllegalArgumentException(hentEnhetBolkUgyldigInput);
        }
    }

    private String getNavn(List<WSDetaljertEnhet> enhetListe, String enhetId) {
        if (enhetListe.size() == 0) {
            throw new NoSuchElementException("Norg returnerte en tom liste når vi spurte etter detaljer om denne enheten: " + enhetId);
        }
        return enhetListe.get(0).getNavn();
    }
}
