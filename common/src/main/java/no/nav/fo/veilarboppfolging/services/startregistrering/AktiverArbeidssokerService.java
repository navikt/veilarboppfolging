package no.nav.fo.veilarboppfolging.services.startregistrering;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarboppfolging.domain.AktiverArbeidssokerData;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.binding.*;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.informasjon.Brukerident;
import no.nav.tjeneste.virksomhet.behandlearbeidssoeker.v1.meldinger.AktiverBrukerRequest;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.Predicates.instanceOf;

@Slf4j
public class AktiverArbeidssokerService {

    private final BehandleArbeidssoekerV1 behandleArbeidssoekerV1;

    public AktiverArbeidssokerService(BehandleArbeidssoekerV1 behandleArbeidssoekerV1) {
        this.behandleArbeidssoekerV1 = behandleArbeidssoekerV1;
    }

    @SuppressWarnings({"unchecked"})
    public void aktiverArbeidssoker(AktiverArbeidssokerData aktiverArbeidssokerData) {
        Brukerident brukerident = new Brukerident();
        brukerident.setBrukerident(aktiverArbeidssokerData.getFnr().getFnr());
        AktiverBrukerRequest request = new AktiverBrukerRequest();
        request.setIdent(brukerident);
        request.setKvalifiseringsgruppekode(aktiverArbeidssokerData.getKvalifiseringsgruppekode());


        Try.run(() -> behandleArbeidssoekerV1.aktiverBruker(request))
                .onFailure((t) -> log.warn("Feil ved aktivering av bruker i arena", t))
                .mapFailure(
                        Case($(instanceOf(AktiverBrukerBrukerFinnesIkke.class)), (t) -> new NotFoundException(t)),
                        Case($(instanceOf(AktiverBrukerBrukerIkkeReaktivert.class)), (t) -> new ServerErrorException(Response.Status.BAD_GATEWAY, t)),
                        Case($(instanceOf(AktiverBrukerBrukerKanIkkeAktiveres.class)), (t) -> new ServerErrorException(Response.Status.BAD_GATEWAY,t)),
                        Case($(instanceOf(AktiverBrukerBrukerManglerArbeidstillatelse.class)), (t) -> new ServerErrorException(Response.Status.BAD_GATEWAY, t)),
                        Case($(instanceOf(AktiverBrukerSikkerhetsbegrensning.class)), (t) -> new NotAuthorizedException(t)),
                        Case($(instanceOf(AktiverBrukerUgyldigInput.class)), (t) -> new BadRequestException(t)),
                        Case($(), (t) -> new InternalServerErrorException(t))
                )
                .get();
    }
}
