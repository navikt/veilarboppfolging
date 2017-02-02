package no.nav.fo.veilarbsituasjon.rest;

import no.nav.fo.veilarbsituasjon.rest.domain.VeilederTilordning;
import no.nav.fo.veilarbsituasjon.services.EndreVeilederService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
@RequestMapping("/tilordneveileder")
public class PortefoljeRessurs {

    @Inject
    EndreVeilederService endreVeilederService;


    @RequestMapping(value = "/", method = RequestMethod.POST, consumes = "application/json")
    public ResponseEntity<?> postVeilederTilordninger(@RequestBody List<VeilederTilordning> veilederTilordninger, HttpServletResponse response) {

        try {
            for (int i = 0; i < veilederTilordninger.size(); i++) {
                endreVeilederService.endreVeileder(veilederTilordninger.get(i));
            }
            return new ResponseEntity<Object>("Veiledere tilordnet", HttpStatus.OK);
        } catch ( Exception e) {
            return new ResponseEntity<Object>("Done", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
