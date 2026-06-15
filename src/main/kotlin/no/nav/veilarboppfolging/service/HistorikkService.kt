package no.nav.veilarboppfolging.service

import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr
import no.nav.veilarboppfolging.controller.response.HistorikkHendelse
import no.nav.veilarboppfolging.controller.response.HistorikkHendelseType
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

    private fun harTilgangTilEnhet(kvp: KvpPeriodeEntity): Boolean {
        return authService.harTilgangTilEnhet(kvp.enhet)
    }

    private fun tilDTO(veilederTilordningHistorikk: VeilederTilordningHistorikkEntity): HistorikkHendelse {
        return HistorikkHendelse(
            type = HistorikkHendelseType.VEILEDER_TILORDNET,
            dato = veilederTilordningHistorikk.sistTilordnet,
            begrunnelse = "Brukeren er tildelt veileder " + veilederTilordningHistorikk.veileder,
            opprettetAv = KodeverkBruker.NAV,
            opprettetAvBrukerId = veilederTilordningHistorikk.tilordnetAvVeileder,
            dialogId = null,
            enhet = null,
            tildeltVeilederId = veilederTilordningHistorikk.veileder,
        )
    }

    private fun tilDTO(oppfolgingsenhetEndringData: OppfolgingsenhetEndringEntity): HistorikkHendelse {
        val enhet = oppfolgingsenhetEndringData.enhet
        return HistorikkHendelse(
            type = HistorikkHendelseType.OPPFOLGINGSENHET_ENDRET,
            dato = oppfolgingsenhetEndringData.endretDato,
            begrunnelse = "Ny oppfølgingsenhet " + enhet,
            opprettetAv = KodeverkBruker.SYSTEM,
            opprettetAvBrukerId = null,
            dialogId = null,
            enhet = enhet,
            tildeltVeilederId = null,
        )
    }

    private fun tilDTO(historikkData: ManuellStatusEntity): HistorikkHendelse {
        return HistorikkHendelse(
            type = if (historikkData.manuell) HistorikkHendelseType.SATT_TIL_MANUELL else HistorikkHendelseType.SATT_TIL_DIGITAL,
            dato = historikkData.dato,
            begrunnelse = historikkData.begrunnelse,
            opprettetAv = historikkData.opprettetAv,
            opprettetAvBrukerId = historikkData.opprettetAvBrukerId,
            dialogId = null,
            enhet = null,
            tildeltVeilederId = null,
        )
    }

    private fun tilDTO(reaktiverOppfolgingHendelseEntity: ReaktiverOppfolgingHendelseEntity): HistorikkHendelse {
        return HistorikkHendelse(
            type = HistorikkHendelseType.REAKTIVERT_OPPFOLGINGSPERIODE,
            dato = reaktiverOppfolgingHendelseEntity.reaktiveringTidspunkt,
            begrunnelse = "Bruker manuelt reaktivert i Arena av veileder",
            opprettetAv = KodeverkBruker.NAV,
            opprettetAvBrukerId = reaktiverOppfolgingHendelseEntity.reaktivertAv,
            dialogId = null,
            enhet = null,
            tildeltVeilederId = null,
        )
    }

    private fun tilDTO(kvp: KvpPeriodeEntity): List<HistorikkHendelse> {
        val kvpStart = HistorikkHendelse(
            type = HistorikkHendelseType.KVP_STARTET,
            dato = kvp.opprettetDato,
            begrunnelse = kvp.opprettetBegrunnelse,
            opprettetAv = kvp.opprettetKodeverkbruker,
            opprettetAvBrukerId = kvp.opprettetAv,
            dialogId = null,
            enhet = null,
            tildeltVeilederId = null,
        )

        if (kvp.avsluttetDato != null) {
            val kvpStopp = HistorikkHendelse(
                type = HistorikkHendelseType.KVP_STOPPET,
                dato = kvp.avsluttetDato,
                begrunnelse = kvp.avsluttetBegrunnelse,
                opprettetAv = kvp.avsluttetKodeverkbruker,
                opprettetAvBrukerId = kvp.avsluttetAv,
                dialogId = null,
                enhet = null,
                tildeltVeilederId = null,
            )
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
            OppfolgingStartBegrunnelse.ARENA_SYNC_IARBS -> "Registrert som sykmeldt uten arbeidsgiver (VURDU) i Arena"
            OppfolgingStartBegrunnelse.MANUELL_REGISTRERING_VEILEDER -> "Veileder startet arbeidsrettet oppfølging på bruker"
            else -> "Startet arbeidsrettet oppfølging på bruker"
        }
    }

    private fun tilDTO(periode: OppfolgingsperiodeEntity): List<HistorikkHendelse> {
        val periodeStart = HistorikkHendelse(
            type = HistorikkHendelseType.STARTET_OPPFOLGINGSPERIODE,
            dato = periode.startDato,
            begrunnelse = getStartetBegrunnelseTekst(periode.startetBegrunnelse, periode.startetAvType),
            opprettetAv = periode.startetAvType?.toKodeverkBruker(),
            opprettetAvBrukerId = periode.startetAv,
            dialogId = null,
            enhet = null,
            tildeltVeilederId = null,
        )

        if (periode.sluttDato != null) {
            val periodeStopp = HistorikkHendelse(
                type = HistorikkHendelseType.AVSLUTTET_OPPFOLGINGSPERIODE,
                dato = periode.sluttDato,
                begrunnelse = periode.begrunnelse,
                opprettetAv = if (periode.avsluttetAv != null) KodeverkBruker.NAV else KodeverkBruker.SYSTEM,
                opprettetAvBrukerId = periode.avsluttetAv,
                dialogId = null,
                enhet = null,
                tildeltVeilederId = null,
            )
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