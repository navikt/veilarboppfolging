package no.nav.fo.veilarbsituasjon.ws;


import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.DigitalKontaktinformasjonV1;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonRequest;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.WSHentDigitalKontaktinformasjonResponse;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.slf4j.LoggerFactory.getLogger;

// TODO dette skal bli en webservice når tjenestespesifikasjonen er klar!
@Component
@Path("/ws/aktivitetsplan")
@Produces(APPLICATION_JSON)
public class AktivitetsplanSituasjonWebService {

    private static final Logger LOG = getLogger(AktivitetsplanSituasjonWebService.class);

    private final DigitalKontaktinformasjonV1 dkifV1;

    public AktivitetsplanSituasjonWebService(DigitalKontaktinformasjonV1 dkifV1) {
        this.dkifV1 = dkifV1;
    }

    @GET
    @Path("/{fnr}")
    public WSHentDigitalKontaktinformasjonResponse hentOppfolgingsStatus(@PathParam("fnr") String fnr) throws Exception {
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
