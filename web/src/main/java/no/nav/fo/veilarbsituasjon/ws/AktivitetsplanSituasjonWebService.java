package no.nav.fo.veilarbsituasjon.ws;


import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonRequest;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonResponse;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

import static org.slf4j.LoggerFactory.getLogger;

// TODO dette skal bli en webservice når tjenestespesifikasjonen er klar!
@RestController
@RequestMapping("/ws/aktivitetsplan")
public class AktivitetsplanSituasjonWebService {

    private static final Logger LOG = getLogger(AktivitetsplanSituasjonWebService.class);

    @Inject
    private DigitalKontaktinformasjonV1 dkifV1;

    @RequestMapping(value = "/{fnr}", method = RequestMethod.GET, produces = "application/json")
    public WSHentDigitalKontaktinformasjonResponse hentOppfolgingsStatus(@PathVariable String fnr) throws Exception{
        try {

            // TODO PK-36884 hent status-flagg db

            // TODO PK-36884 hent status ws

            // TODO PK-36884 lagre status

            WSHentDigitalKontaktinformasjonRequest wsHentDigitalKontaktinformasjonRequest = new WSHentDigitalKontaktinformasjonRequest().withPersonident(fnr);
            WSHentDigitalKontaktinformasjonResponse wsHentDigitalKontaktinformasjonResponse = dkifV1.hentDigitalKontaktinformasjon(wsHentDigitalKontaktinformasjonRequest);

            // TODO PK-36884 hent og sett manuellflagg

            return wsHentDigitalKontaktinformasjonResponse;
        } catch (Exception e) {
            LOG.error("Det skjedde en uventet feil mot DKIF.");
            throw e;
        }
    }

}
