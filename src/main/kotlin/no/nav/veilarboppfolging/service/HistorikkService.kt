package no.nav.veilarboppfolging.service

import lombok.RequiredArgsConstructor
import lombok.SneakyThrows
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.controller.response.HistorikkHendelse
import no.nav.veilarboppfolging.oppfolgingsbruker.StartetAvType
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingStartBegrunnelse
import no.nav.veilarboppfolging.repository.*
import no.nav.veilarboppfolging.repository.entity.*
import no.nav.veilarboppfolging.repository.enums.KodeverkBruker
import no.nav.veilarboppfolging.utils.KvpUtils
import org.springframework.stereotype.Service
import java.util.*
import java.util.function.Supplier

@Service
@RequiredArgsConstructor
class HistorikkService(
    private val authService: AuthService,
    private val kvpRepository: KvpRepository,
    private val veilederHistorikkRepository: VeilederHistorikkRepository,
    private val oppfolgingsenhetHistorikkRepository: OppfolgingsenhetHistorikkRepository,
    private val oppfolgingsPeriodeRepository: OppfolgingsPeriodeRepository,
    private val manuellStatusService: ManuellStatusService,
    private val reaktiveringRepository: ReaktiveringRepository
) {

    fun hentInstillingsHistorikk(fnr: Fnr?): List<HistorikkHendelse> {
        val aktorId = authService.getAktorIdOrThrow(fnr)
        return hentInstillingHistorikk(aktorId)
            .filter { historikk -> Objects.nonNull(historikk) }
            .flatten()
    }

    @SneakyThrows
    private fun harTilgangTilEnhet(kvp: KvpPeriodeEntity): Boolean {
        return authService.harTilgangTilEnhet(kvp.enhet)
    }

    private fun tilDTO(veilederTilordningHistorikk: VeilederTilordningHistorikkEntity): HistorikkHendelse {
        return HistorikkHendelse.builder()
            .type(HistorikkHendelse.Type.VEILEDER_TILORDNET)
            .begrunnelse("Brukeren er tildelt veileder " + veilederTilordningHistorikk.veileder)
            .veileder(veilederTilordningHistorikk.veileder)
            .dato(veilederTilordningHistorikk.sistTilordnet)
            .opprettetAv(KodeverkBruker.NAV)
            .opprettetAvBrukerId(veilederTilordningHistorikk.tilordnetAvVeileder)
            .build()
    }

    private fun tilDTO(oppfolgingsenhetEndringData: OppfolgingsenhetEndringEntity): HistorikkHendelse {
        val enhet = oppfolgingsenhetEndringData.enhet
        return HistorikkHendelse.builder()
            .type(HistorikkHendelse.Type.OPPFOLGINGSENHET_ENDRET)
            .enhet(enhet)
            .begrunnelse("Ny oppfølgingsenhet " + enhet)
            .dato(oppfolgingsenhetEndringData.endretDato)
            .opprettetAv(KodeverkBruker.SYSTEM)
            .build()
    }

    private fun tilDTO(historikkData: ManuellStatusEntity): HistorikkHendelse {
        return HistorikkHendelse.builder()
            .type(if (historikkData.isManuell) HistorikkHendelse.Type.SATT_TIL_MANUELL else HistorikkHendelse.Type.SATT_TIL_DIGITAL)
            .begrunnelse(historikkData.begrunnelse)
            .dato(historikkData.dato)
            .opprettetAv(historikkData.opprettetAv)
            .opprettetAvBrukerId(historikkData.opprettetAvBrukerId)
            .build()
    }

    private fun tilDTO(reaktiverOppfolgingHendelseEntity: ReaktiverOppfolgingHendelseEntity): HistorikkHendelse {
        return HistorikkHendelse.builder()
            .type(HistorikkHendelse.Type.REAKTIVERT_OPPFOLGINGSPERIODE)
            .begrunnelse(getStartetBegrunnelseTekst(OppfolgingStartBegrunnelse.REAKTIVERT_OPPFØLGING, null))
            .dato(reaktiverOppfolgingHendelseEntity.reaktiveringTidspunkt)
            .opprettetAv(KodeverkBruker.NAV)
            .opprettetAvBrukerId(reaktiverOppfolgingHendelseEntity.reaktivertAv)
            .build()
    }

    private fun tilDTO(kvp: KvpPeriodeEntity): List<HistorikkHendelse> {
        val kvpStart = HistorikkHendelse.builder()
            .type(HistorikkHendelse.Type.KVP_STARTET)
            .begrunnelse(kvp.opprettetBegrunnelse)
            .dato(kvp.opprettetDato)
            .opprettetAv(kvp.opprettetKodeverkbruker)
            .opprettetAvBrukerId(kvp.opprettetAv)
            .build()

        if (kvp.avsluttetDato != null) {
            val kvpStopp = HistorikkHendelse.builder()
                .type(HistorikkHendelse.Type.KVP_STOPPET)
                .begrunnelse(kvp.avsluttetBegrunnelse)
                .dato(kvp.avsluttetDato)
                .opprettetAv(kvp.avsluttetKodeverkbruker)
                .opprettetAvBrukerId(kvp.avsluttetAv)
                .build()
            return listOf(kvpStart, kvpStopp)
        }
        return listOf(kvpStart)
    }

    private fun getStartetBegrunnelseTekst(begrunnelse: OppfolgingStartBegrunnelse?, startetAvType: StartetAvType?): String {
        return when (begrunnelse) {
            OppfolgingStartBegrunnelse.ARBEIDSSOKER_REGISTRERING -> {
                when (startetAvType) {
                    StartetAvType.VEILEDER -> "Bruker ble registrert som arbeidssøker av veileder"
                    StartetAvType.BRUKER -> "Bruker registrerte seg som arbeidssøker"
                    else -> "Bruker ble registrert som arbeidssøker"
                }
            }
            OppfolgingStartBegrunnelse.ARENA_SYNC_ARBS -> "Registrert som arbeidssøker i arena"
            OppfolgingStartBegrunnelse.ARENA_SYNC_IARBS -> "Registrert 14a vedtak i arena eller sykmeldt uten arbeidsgiver (VURDU)"
            OppfolgingStartBegrunnelse.MANUELL_REGISTRERING_VEILEDER -> "Veileder startet arbeidsrettet oppfølging på bruker"
            OppfolgingStartBegrunnelse.REAKTIVERT_OPPFØLGING -> "Arbeidsrettet oppfølging ble reaktivert"
            else -> "Startet arbeidsrettet oppfølging på bruker"
        }
    }

    private fun tilDTO(periode: OppfolgingsperiodeEntity): List<HistorikkHendelse> {
        val periodeStart = HistorikkHendelse.builder()
            .type(HistorikkHendelse.Type.STARTET_OPPFOLGINGSPERIODE)
            .begrunnelse(getStartetBegrunnelseTekst(periode.startetBegrunnelse, periode.startetAvType))
            .dato(periode.startDato)
            .opprettetAv(periode.startetAvType?.toKodeverkBruker())
            .opprettetAvBrukerId(periode.startetAv)
            .build()

        if (periode.sluttDato != null) {
            val periodeStopp = HistorikkHendelse.builder()
                .type(HistorikkHendelse.Type.AVSLUTTET_OPPFOLGINGSPERIODE)
                .begrunnelse(periode.begrunnelse)
                .dato(periode.sluttDato)
                .opprettetAv(if (periode.avsluttetAv != null) KodeverkBruker.NAV else KodeverkBruker.SYSTEM)
                .opprettetAvBrukerId(periode.avsluttetAv)
                .build()
            return listOf(periodeStart, periodeStopp)
        }
        return listOf(periodeStart)
    }

    private fun hentInstillingHistorikk(aktorId: AktorId): List<List<HistorikkHendelse>> {
        val kvpHistorikk = kvpRepository.hentKvpHistorikk(aktorId)

        fun sjekkTilgangGittKvp(historikk: HistorikkHendelse): Boolean {
            return KvpUtils.sjekkTilgangGittKvp(
                authService,
                kvpHistorikk,
                Supplier { historikk.dato })
        }

        val veilederTilordningerInnstillingHistorikk = veilederHistorikkRepository
            .hentTilordnedeVeiledereForAktorId(aktorId)
            .map { veilederTilordningHistorikk -> tilDTO(veilederTilordningHistorikk) }
            .filter(::sjekkTilgangGittKvp)

        val kvpInnstillingHistorikk = kvpHistorikk
            .filter { kvp -> harTilgangTilEnhet(kvp) }
            .map { kvp -> tilDTO(kvp) }
            .flatten()

        val oppfolgingsperiodeHistorikk = oppfolgingsPeriodeRepository.hentOppfolgingsperioder(aktorId)
            .map { periode -> tilDTO(periode) }
            .flatten()

        val manuellInnstillingHistorikk = manuellStatusService.hentManuellStatusHistorikk(aktorId)
            .map { historikkData -> tilDTO(historikkData) }
            .filter(::sjekkTilgangGittKvp)

        val oppfolgingsEnhetEndringHistorikk =
            oppfolgingsenhetHistorikkRepository.hentOppfolgingsenhetEndringerForAktorId(aktorId)
                .mapNotNull { oppfolgingsenhetEndringData -> oppfolgingsenhetEndringData?.let(::tilDTO) }
                .filter(::sjekkTilgangGittKvp)

        val reaktiveringHistorikk = reaktiveringRepository.hentReaktiveringer(aktorId)
            .map  { reaktiverOppfolgingHendelseEntity -> tilDTO(reaktiverOppfolgingHendelseEntity) }

        return listOf(
            kvpInnstillingHistorikk,
            oppfolgingsperiodeHistorikk,
            manuellInnstillingHistorikk,
            veilederTilordningerInnstillingHistorikk,
            oppfolgingsEnhetEndringHistorikk,
            reaktiveringHistorikk
        )
    }
}

fun StartetAvType.toKodeverkBruker(): KodeverkBruker {
    return when (this) {
        StartetAvType.VEILEDER -> KodeverkBruker.NAV
        StartetAvType.SYSTEM -> KodeverkBruker.SYSTEM
        StartetAvType.BRUKER -> KodeverkBruker.EKSTERN
    }
}