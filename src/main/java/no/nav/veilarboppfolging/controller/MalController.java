package no.nav.veilarboppfolging.controller;

import no.nav.veilarboppfolging.controller.domain.Mal;
import no.nav.veilarboppfolging.domain.MalData;
import no.nav.veilarboppfolging.utils.FnrParameterUtil;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import java.util.List;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping("/oppfolging")
public class MalController {

    @GET
    @Path("/mal")
    public Mal hentMal() throws PepException {
        return tilDto(malService.hentMal(getFnr()));
    }

    @GET
    @Path("/malListe")
    public List<Mal> hentMalListe() throws PepException {
        List<MalData> malDataList = malService.hentMalList(getFnr());
        return malDataList.stream()
                .map(this::tilDto)
                .collect(toList());
    }

    @POST
    @Path("/mal")
    public Mal oppdaterMal(Mal mal) throws PepException {
        String endretAvVeileder = FnrParameterUtil.erEksternBruker()? null : getUid();
        return tilDto(malService.oppdaterMal(mal.getMal(), getFnr(), endretAvVeileder));
    }

}
