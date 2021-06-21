package no.nav.veilarboppfolging.service;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.InnstillingsHistorikk;
import no.nav.veilarboppfolging.repository.*;
import no.nav.veilarboppfolging.repository.entity.*;
import no.nav.veilarboppfolging.utils.KvpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static no.nav.veilarboppfolging.controller.response.InnstillingsHistorikk.Type.*;
import static no.nav.veilarboppfolging.repository.enums.KodeverkBruker.NAV;
import static no.nav.veilarboppfolging.repository.enums.KodeverkBruker.SYSTEM;

@Service
public class HistorikkService {

    private final AuthService authService;

    private final KvpRepository kvpRepository;

    private final VeilederHistorikkRepository veilederHistorikkRepository;

    private final OppfolgingsenhetHistorikkRepository oppfolgingsenhetHistorikkRepository;

    private final EskaleringsvarselRepository eskaleringsvarselRepository;

    private final OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;

    private final ManuellStatusService manuellStatusService;

    @Autowired
    public HistorikkService(
            AuthService authService,
            KvpRepository kvpRepository,
            VeilederHistorikkRepository veilederHistorikkRepository,
            OppfolgingsenhetHistorikkRepository oppfolgingsenhetHistorikkRepository,
            EskaleringsvarselRepository eskaleringsvarselRepository,
            OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository,
            ManuellStatusService manuellStatusService
    ) {
        this.authService = authService;
        this.kvpRepository = kvpRepository;
        this.veilederHistorikkRepository = veilederHistorikkRepository;
        this.oppfolgingsenhetHistorikkRepository = oppfolgingsenhetHistorikkRepository;
        this.eskaleringsvarselRepository = eskaleringsvarselRepository;
        this.oppfolgingsPeriodeRepository = oppfolgingsPeriodeRepository;
        this.manuellStatusService = manuellStatusService;
    }

    public List<InnstillingsHistorikk> hentInstillingsHistorikk(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);
        return hentInstillingHistorikk(aktorId).filter(Objects::nonNull).flatMap(s -> s).collect(Collectors.toList());
    }

    @SneakyThrows
    private boolean harTilgangTilEnhet(KvpPeriodeEntity kvp) {
        return authService.harTilgangTilEnhet(kvp.getEnhet());
    }

    private InnstillingsHistorikk tilDTO(VeilederTilordningHistorikkEntity veilederTilordningHistorikk) {
        return InnstillingsHistorikk.builder()
                .type(VEILEDER_TILORDNET)
                .begrunnelse("Brukeren er tildelt veileder " +  veilederTilordningHistorikk.getVeileder())
                .dato(veilederTilordningHistorikk.getSistTilordnet())
                .opprettetAv(NAV)
                .build();
    }

    private InnstillingsHistorikk tilDTO(OppfolgingsenhetEndringEntity oppfolgingsenhetEndringData) {
        String enhet = oppfolgingsenhetEndringData.getEnhet();
        return InnstillingsHistorikk.builder()
                .type(OPPFOLGINGSENHET_ENDRET)
                .enhet(enhet)
                .begrunnelse("Ny oppfølgingsenhet " + enhet)
                .dato(oppfolgingsenhetEndringData.getEndretDato())
                .opprettetAv(SYSTEM)
                .build();
    }

    private InnstillingsHistorikk tilDTO(OppfolgingsperiodeEntity oppfolgingsperiode) {
        String veilderId = oppfolgingsperiode.getVeileder();
        return InnstillingsHistorikk.builder()
                .type(AVSLUTTET_OPPFOLGINGSPERIODE)
                .begrunnelse(oppfolgingsperiode.getBegrunnelse())
                .dato(oppfolgingsperiode.getSluttDato())
                .opprettetAv(veilderId != null ? NAV : SYSTEM)
                .opprettetAvBrukerId(veilderId)
                .build();
    }

    private InnstillingsHistorikk tilDTO(ManuellStatusEntity historikkData) {
        return InnstillingsHistorikk.builder()
                .type(historikkData.isManuell() ? SATT_TIL_MANUELL : SATT_TIL_DIGITAL)
                .begrunnelse(historikkData.getBegrunnelse())
                .dato(historikkData.getDato())
                .opprettetAv(historikkData.getOpprettetAv())
                .opprettetAvBrukerId(historikkData.getOpprettetAvBrukerId())
                .build();
    }

    private List<InnstillingsHistorikk> tilDTO(EskaleringsvarselEntity data) {
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

    private List<InnstillingsHistorikk> tilDTO(KvpPeriodeEntity kvp) {
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

    private Stream<Stream<InnstillingsHistorikk>> hentInstillingHistorikk (AktorId aktorId) {
        List<KvpPeriodeEntity> kvpHistorikk = kvpRepository.hentKvpHistorikk(aktorId);

        Stream <InnstillingsHistorikk> veilederTilordningerInnstillingHistorikk = null;

        veilederTilordningerInnstillingHistorikk = veilederHistorikkRepository.hentTilordnedeVeiledereForAktorId(aktorId)
                .stream()
                .map(this::tilDTO)
                .filter((historikk) -> KvpUtils.sjekkTilgangGittKvp(authService, kvpHistorikk, historikk::getDato));

        Stream<InnstillingsHistorikk> kvpInnstillingHistorikk = kvpHistorikk.stream()
                .filter(this::harTilgangTilEnhet)
                .map(this::tilDTO).flatMap(List::stream);

        Stream<InnstillingsHistorikk> avluttetOppfolgingInnstillingHistorikk = oppfolgingsPeriodeRepository.hentAvsluttetOppfolgingsperioder(aktorId)
                .stream()
                .map(this::tilDTO);

        Stream<InnstillingsHistorikk> manuellInnstillingHistorikk = manuellStatusService.hentManuellStatusHistorikk(aktorId)
                .stream()
                .map(this::tilDTO)
                .filter((historikk) -> KvpUtils.sjekkTilgangGittKvp(authService, kvpHistorikk, historikk::getDato));

        Stream <InnstillingsHistorikk> eskaleringInnstillingHistorikk = eskaleringsvarselRepository.history(aktorId).stream()
                .map(this::tilDTO)
                .flatMap(List::stream)
                .filter((historikk) -> KvpUtils.sjekkTilgangGittKvp(authService, kvpHistorikk, historikk::getDato));

        Stream <InnstillingsHistorikk> enhetEndringHistorikk = oppfolgingsenhetHistorikkRepository.hentOppfolgingsenhetEndringerForAktorId(aktorId)
                .stream()
                .map(this::tilDTO)
                .filter((historikk) -> KvpUtils.sjekkTilgangGittKvp(authService, kvpHistorikk, historikk::getDato));


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
