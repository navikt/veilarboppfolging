package no.nav.fo.veilarbsituasjon.mappers;


import no.nav.fo.veilarbsituasjon.domain.OppfolgingskontraktData;
import no.nav.fo.veilarbsituasjon.domain.OppfolgingskontraktResponse;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.Oppfoelgingskontrakt;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.informasjon.ServiceGruppe;
import no.nav.tjeneste.virksomhet.oppfoelging.v1.meldinger.HentOppfoelgingskontraktListeResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;


@Component
public class OppfolgingMapper {

    public OppfolgingskontraktResponse tilOppfolgingskontrakt(HentOppfoelgingskontraktListeResponse response) {

        final List<OppfolgingskontraktData> oppfolgingskontrakter = response.getOppfoelgingskontraktListe().stream()
                .map(tilOppfolgingskontrakt).collect(Collectors.toList());

        return new OppfolgingskontraktResponse(oppfolgingskontrakter);
    }

    private final static Function<Oppfoelgingskontrakt, OppfolgingskontraktData> tilOppfolgingskontrakt =
            wsOppfolgingskontrakt -> new OppfolgingskontraktData()
                    .withInnsatsgruppe(wsOppfolgingskontrakt
                            .getGjelderBruker()
                            .getServicegruppe()
                            .stream()
                            .map(ServiceGruppe::getServiceGruppe).collect(Collectors.toList()))
                    .withStatus(wsOppfolgingskontrakt.getStatus());
}
