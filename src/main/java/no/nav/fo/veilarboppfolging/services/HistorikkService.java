package no.nav.fo.veilarboppfolging.services;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.apiapp.security.veilarbabac.VeilarbAbacPepClient;
import no.nav.dialogarena.aktor.AktorService;
import no.nav.fo.veilarboppfolging.db.KvpRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingRepository;
import no.nav.fo.veilarboppfolging.db.OppfolgingsenhetHistorikkRepository;
import no.nav.fo.veilarboppfolging.db.VeilederHistorikkRepository;
import no.nav.fo.veilarboppfolging.domain.*;
import no.nav.fo.veilarboppfolging.utils.KvpUtils;
import no.nav.sbl.featuretoggle.unleash.UnleashService;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static no.nav.fo.veilarboppfolging.domain.InnstillingsHistorikk.Type.*;
import static no.nav.fo.veilarboppfolging.domain.KodeverkBruker.NAV;
import static no.nav.fo.veilarboppfolging.domain.KodeverkBruker.SYSTEM;

@Component
public class HistorikkService {

    @Inject
    private AktorService aktorService;

    @Inject
    private KvpRepository kvpRepository;

    @Inject
    private VeilarbAbacPepClient pepClient;

    @Inject
    private OppfolgingRepository oppfolgingRepository;

    @Inject
    private VeilederHistorikkRepository veilederHistorikkRepository;

    @Inject
    private UnleashService unleashService;

    @Inject
    private OppfolgingsenhetHistorikkRepository oppfolgingsenhetHistorikkRepository;


    public List<InnstillingsHistorikk> hentInstillingsHistorikk(String fnr) {
        String aktorId = aktorService.getAktorId(fnr)
                .orElseThrow(() -> new IllegalArgumentException("Fant ikke aktør for fnr: " + fnr));

        return hentInstillingHistorikk(aktorId).filter(Objects::nonNull).flatMap(s -> s).collect(Collectors.toList());
    }

    @SneakyThrows
    private boolean harTilgangTilEnhet(Kvp kvp) {
        return pepClient.harTilgangTilEnhet(kvp.getEnhet());
    }

    private InnstillingsHistorikk tilDTO(VeilederTilordningerData veilederTilordningerData) {
        return InnstillingsHistorikk.builder()
                .type(VEILEDER_TILORDNET)
                .begrunnelse("Brukeren er tildelt veileder " +  veilederTilordningerData.getVeileder())
                .dato(veilederTilordningerData.getSistTilordnet())
                .opprettetAv(NAV)
                .build();
    }

    private InnstillingsHistorikk tilDTO(OppfolgingsenhetEndringData oppfolgingsenhetEndringData) {
        return InnstillingsHistorikk.builder()
                .type(OPPFOLGINGSENHET_ENDRET)
                .begrunnelse("Ny oppfølgingsenhet " + oppfolgingsenhetEndringData.getEnhet())
                .dato(oppfolgingsenhetEndringData.getEndretDato())
                .opprettetAv(NAV)
                .build();
    }

    private InnstillingsHistorikk tilDTO(Oppfolgingsperiode oppfolgingsperiode) {
        String veilderId = oppfolgingsperiode.getVeileder();
        return InnstillingsHistorikk.builder()
                .type(AVSLUTTET_OPPFOLGINGSPERIODE)
                .begrunnelse(oppfolgingsperiode.getBegrunnelse())
                .dato(oppfolgingsperiode.getSluttDato())
                .opprettetAv(veilderId != null ? NAV : SYSTEM)
                .opprettetAvBrukerId(veilderId)
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
                .opprettetAv(kvp.getOpprettetKodeverkbruker())
                .opprettetAvBrukerId(kvp.getOpprettetAv())
                .build();

        if (kvp.getAvsluttetDato() != null) {
            InnstillingsHistorikk kvpStopp = InnstillingsHistorikk.builder()
                    .type(KVP_STOPPET)
                    .begrunnelse(kvp.getAvsluttetBegrunnelse())
                    .dato(kvp.getAvsluttetDato())
                    .opprettetAv(kvp.getAvsluttetKodeverkbruker())
                    .opprettetAvBrukerId(kvp.getAvsluttetAv())
                    .build();
            return Arrays.asList(kvpStart, kvpStopp);
        }
        return singletonList(kvpStart);
    }

    private Stream<Stream<InnstillingsHistorikk>> hentInstillingHistorikk (String aktorId) {
        List<Kvp> kvpHistorikk = kvpRepository.hentKvpHistorikk(aktorId);

        Stream <InnstillingsHistorikk> veilederTilordningerInnstillingHistorikk = null;

        if(unleashService.isEnabled("veilarboppfolging.tildel_veileder")) {
            veilederTilordningerInnstillingHistorikk =  veilederHistorikkRepository.hentTilordnedeVeiledereForAktorId(aktorId).stream()
                    .map(this::tilDTO)
                    .filter((historikk) -> KvpUtils.sjekkTilgangGittKvp(pepClient, kvpHistorikk, historikk::getDato));
        }

        Stream<InnstillingsHistorikk> kvpInnstillingHistorikk = kvpHistorikk.stream()
                .filter(this::harTilgangTilEnhet)
                .map(this::tilDTO).flatMap(List::stream);

        Stream<InnstillingsHistorikk> avluttetOppfolgingInnstillingHistorikk = oppfolgingRepository.hentAvsluttetOppfolgingsperioder(aktorId)
                .stream()
                .map(this::tilDTO);

        Stream<InnstillingsHistorikk> manuellInnstillingHistorikk = oppfolgingRepository.hentManuellHistorikk(aktorId)
                .stream()
                .map(this::tilDTO)
                .filter((historikk) -> KvpUtils.sjekkTilgangGittKvp(pepClient, kvpHistorikk, historikk::getDato));

        Stream <InnstillingsHistorikk> eskaleringInnstillingHistorikk = oppfolgingRepository.hentEskaleringhistorikk(aktorId).stream()
                .map(this::tilDTO)
                .flatMap(List::stream)
                .filter((historikk) -> KvpUtils.sjekkTilgangGittKvp(pepClient, kvpHistorikk, historikk::getDato));

        Stream <InnstillingsHistorikk> enhetEndringHistorikk = oppfolgingsenhetHistorikkRepository.hentOppfolgingsenhetEndringerForAktorId(aktorId)
                .stream()
                .map(this::tilDTO)
                .filter((historikk) -> KvpUtils.sjekkTilgangGittKvp(pepClient, kvpHistorikk, historikk::getDato));


        return Stream.of(
                kvpInnstillingHistorikk,
                avluttetOppfolgingInnstillingHistorikk,
                manuellInnstillingHistorikk,
                eskaleringInnstillingHistorikk,
                veilederTilordningerInnstillingHistorikk,
                enhetEndringHistorikk
        );
    }
}
