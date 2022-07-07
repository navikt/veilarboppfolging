package no.nav.veilarboppfolging.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.veilarboppfolging.controller.response.HistorikkHendelse;
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
import static no.nav.veilarboppfolging.controller.response.HistorikkHendelse.Type.*;
import static no.nav.veilarboppfolging.repository.enums.KodeverkBruker.NAV;
import static no.nav.veilarboppfolging.repository.enums.KodeverkBruker.SYSTEM;

@Service
@RequiredArgsConstructor
public class HistorikkService {

    private final AuthService authService;

    private final KvpRepository kvpRepository;

    private final VeilederHistorikkRepository veilederHistorikkRepository;

    private final OppfolgingsenhetHistorikkRepository oppfolgingsenhetHistorikkRepository;

    private final OppfolgingsPeriodeRepository oppfolgingsPeriodeRepository;

    private final ManuellStatusService manuellStatusService;

    public List<HistorikkHendelse> hentInstillingsHistorikk(Fnr fnr) {
        AktorId aktorId = authService.getAktorIdOrThrow(fnr);
        return hentInstillingHistorikk(aktorId).filter(Objects::nonNull).flatMap(s -> s).collect(Collectors.toList());
    }

    @SneakyThrows
    private boolean harTilgangTilEnhet(KvpPeriodeEntity kvp) {
        return authService.harTilgangTilEnhet(kvp.getEnhet());
    }

    private HistorikkHendelse tilDTO(VeilederTilordningHistorikkEntity veilederTilordningHistorikk) {
        return HistorikkHendelse.builder()
                .type(VEILEDER_TILORDNET)
                .begrunnelse("Brukeren er tildelt veileder " +  veilederTilordningHistorikk.getVeileder())
                .dato(veilederTilordningHistorikk.getSistTilordnet())
                .opprettetAv(NAV)
                .build();
    }

    private HistorikkHendelse tilDTO(OppfolgingsenhetEndringEntity oppfolgingsenhetEndringData) {
        String enhet = oppfolgingsenhetEndringData.getEnhet();
        return HistorikkHendelse.builder()
                .type(OPPFOLGINGSENHET_ENDRET)
                .enhet(enhet)
                .begrunnelse("Ny oppfølgingsenhet " + enhet)
                .dato(oppfolgingsenhetEndringData.getEndretDato())
                .opprettetAv(SYSTEM)
                .build();
    }

    private HistorikkHendelse tilDTO(OppfolgingsperiodeEntity oppfolgingsperiode) {
        String veilderId = oppfolgingsperiode.getVeileder();
        return HistorikkHendelse.builder()
                .type(AVSLUTTET_OPPFOLGINGSPERIODE)
                .begrunnelse(oppfolgingsperiode.getBegrunnelse())
                .dato(oppfolgingsperiode.getSluttDato())
                .opprettetAv(veilderId != null ? NAV : SYSTEM)
                .opprettetAvBrukerId(veilderId)
                .build();
    }

    private HistorikkHendelse tilDTO(ManuellStatusEntity historikkData) {
        return HistorikkHendelse.builder()
                .type(historikkData.isManuell() ? SATT_TIL_MANUELL : SATT_TIL_DIGITAL)
                .begrunnelse(historikkData.getBegrunnelse())
                .dato(historikkData.getDato())
                .opprettetAv(historikkData.getOpprettetAv())
                .opprettetAvBrukerId(historikkData.getOpprettetAvBrukerId())
                .build();
    }

    private List<HistorikkHendelse> tilDTO(KvpPeriodeEntity kvp) {
        HistorikkHendelse kvpStart = HistorikkHendelse.builder()
                .type(KVP_STARTET)
                .begrunnelse(kvp.getOpprettetBegrunnelse())
                .dato(kvp.getOpprettetDato())
                .opprettetAv(kvp.getOpprettetKodeverkbruker())
                .opprettetAvBrukerId(kvp.getOpprettetAv())
                .build();

        if (kvp.getAvsluttetDato() != null) {
            HistorikkHendelse kvpStopp = HistorikkHendelse.builder()
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

    private Stream<Stream<HistorikkHendelse>> hentInstillingHistorikk (AktorId aktorId) {
        List<KvpPeriodeEntity> kvpHistorikk = kvpRepository.hentKvpHistorikk(aktorId);

        Stream <HistorikkHendelse> veilederTilordningerInnstillingHistorikk = null;

        veilederTilordningerInnstillingHistorikk = veilederHistorikkRepository.hentTilordnedeVeiledereForAktorId(aktorId)
                .stream()
                .map(this::tilDTO)
                .filter((historikk) -> KvpUtils.sjekkTilgangGittKvp(authService, kvpHistorikk, historikk::getDato));

        Stream<HistorikkHendelse> kvpInnstillingHistorikk = kvpHistorikk.stream()
                .filter(this::harTilgangTilEnhet)
                .map(this::tilDTO).flatMap(List::stream);

        Stream<HistorikkHendelse> avluttetOppfolgingInnstillingHistorikk = oppfolgingsPeriodeRepository.hentAvsluttetOppfolgingsperioder(aktorId)
                .stream()
                .map(this::tilDTO);

        Stream<HistorikkHendelse> manuellInnstillingHistorikk = manuellStatusService.hentManuellStatusHistorikk(aktorId)
                .stream()
                .map(this::tilDTO)
                .filter((historikk) -> KvpUtils.sjekkTilgangGittKvp(authService, kvpHistorikk, historikk::getDato));

        Stream <HistorikkHendelse> enhetEndringHistorikk = oppfolgingsenhetHistorikkRepository.hentOppfolgingsenhetEndringerForAktorId(aktorId)
                .stream()
                .map(this::tilDTO)
                .filter((historikk) -> KvpUtils.sjekkTilgangGittKvp(authService, kvpHistorikk, historikk::getDato));


        return Stream.of(
                kvpInnstillingHistorikk,
                avluttetOppfolgingInnstillingHistorikk,
                manuellInnstillingHistorikk,
                veilederTilordningerInnstillingHistorikk,
                enhetEndringHistorikk
        );
    }
}
