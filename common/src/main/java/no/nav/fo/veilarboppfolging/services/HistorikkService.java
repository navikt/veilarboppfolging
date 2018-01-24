package no.nav.fo.veilarboppfolging.services;

import lombok.val;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.KvpRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.domain.*;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static no.nav.fo.veilarboppfolging.domain.InnstillingsHistorikk.Type.*;
import static no.nav.fo.veilarboppfolging.domain.KodeverkBruker.NAV;

@Component
public class HistorikkService {

    @Inject
    private AktorService aktorService;

    @Inject
    private KvpRepository kvpRepository;

    @Inject
    private EnhetPepClient enhetPepClient;

    @Inject
    private OppfolgingRepository oppfolgingRepository;


    public List<InnstillingsHistorikk> hentInstillingsHistorikk(String fnr) {
        String aktorId = aktorService.getAktorId(fnr)
                .orElseThrow(() -> new IllegalArgumentException("Fant ikke aktør for fnr: " + fnr));

        List<Kvp> kvpHistorikk = kvpRepository.hentKvpHistorikk(aktorId);

        return Stream.of(
                kvpHistorikk.stream()
                        .filter((kvp) -> enhetPepClient.harTilgang(kvp.getEnhet()))
                        .map(this::tilDTO).flatMap(List::stream),
                oppfolgingRepository.hentAvsluttetOppfolgingsperioder(aktorId).stream()
                        .map(this::tilDTO),
                oppfolgingRepository.hentManuellHistorikk(aktorId).stream()
                        .map(this::tilDTO)
                        .filter((dto) -> kvpHistorikkSjekk(kvpHistorikk, dto)),
                oppfolgingRepository.hentEskaleringhistorikk(aktorId).stream()
                        .map(this::tilDTO)
                        .flatMap(List::stream)
                        .filter((dto) -> kvpHistorikkSjekk(kvpHistorikk, dto))
        ).flatMap(s -> s).collect(Collectors.toList());
    }


    private boolean between(Date start, Date stop, Date date) {
        return !date.before(start) && (stop == null || !date.after(stop));
    }

    private boolean kvpHistorikkSjekk(List<Kvp> kvpList, InnstillingsHistorikk innstillingsHistorikk) {
        for (Kvp kvp : kvpList) {
            if (between(kvp.getOpprettetDato(), kvp.getAvsluttetDato(), innstillingsHistorikk.getDato())) {
                return enhetPepClient.harTilgang(kvp.getEnhet());
            }
        }
        return true;
    }

    private InnstillingsHistorikk tilDTO(Oppfolgingsperiode oppfolgingsperiode) {
        return InnstillingsHistorikk.builder()
                .type(AVSLUTTET_OPPFOLGINGSPERIODE)
                .begrunnelse(oppfolgingsperiode.getBegrunnelse())
                .dato(oppfolgingsperiode.getSluttDato())
                .opprettetAv(NAV)
                .opprettetAvBrukerId(oppfolgingsperiode.getVeileder())
                .build();
    }

    private InnstillingsHistorikk tilDTO(ManuellStatus historikkData) {
        return InnstillingsHistorikk.builder()
                .type(historikkData.isManuell() ? SATT_TIL_MANUELL : SATT_TIL_DIGITAL)
                .begrunnelse(historikkData.getBegrunnelse())
                .dato(historikkData.getDato())
                .opprettetAv(historikkData.getOpprettetAv())
                .opprettetAvBrukerId(historikkData.getOpprettetAvBrukerId())
                .build();
    }

    private List<InnstillingsHistorikk> tilDTO(EskaleringsvarselData data) {
        val harAvsluttetEskalering = data.getAvsluttetDato() != null;

        val startetEskalering = InnstillingsHistorikk
                .builder()
                .type(ESKALERING_STARTET)
                .dato(data.getOpprettetDato())
                .begrunnelse(data.getOpprettetBegrunnelse())
                .opprettetAv(NAV)
                .opprettetAvBrukerId(data.getOpprettetAv())
                .dialogId(data.getTilhorendeDialogId())
                .build();

        if (harAvsluttetEskalering) {
            val stoppetEskalering = InnstillingsHistorikk
                    .builder()
                    .type(ESKALERING_STOPPET)
                    .dato(data.getAvsluttetDato())
                    .begrunnelse(data.getAvsluttetBegrunnelse())
                    .opprettetAv(NAV)
                    .opprettetAvBrukerId(data.getAvsluttetAv())
                    .dialogId(data.getTilhorendeDialogId())
                    .build();
            return Arrays.asList(startetEskalering, stoppetEskalering);
        } else {
            return singletonList(startetEskalering);
        }
    }

    private List<InnstillingsHistorikk> tilDTO(Kvp kvp) {
        InnstillingsHistorikk kvpStart = InnstillingsHistorikk.builder()
                .type(KVP_STARTET)
                .begrunnelse(kvp.getOpprettetBegrunnelse())
                .dato(kvp.getOpprettetDato())
                .opprettetAv(NAV)
                .opprettetAvBrukerId(kvp.getOpprettetAv())
                .build();

        if (kvp.getAvsluttetDato() != null) {
            InnstillingsHistorikk kvpStopp = InnstillingsHistorikk.builder()
                    .type(KVP_STOPPET)
                    .begrunnelse(kvp.getAvsluttetBegrunnelse())
                    .dato(kvp.getAvsluttetDato())
                    .opprettetAv(NAV)
                    .opprettetAvBrukerId(kvp.getAvsluttetAv())
                    .build();
            return Arrays.asList(kvpStart, kvpStopp);
        }
        return singletonList(kvpStart);
    }
}
