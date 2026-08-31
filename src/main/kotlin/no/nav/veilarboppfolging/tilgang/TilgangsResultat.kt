package no.nav.veilarboppfolging.tilgang

import no.nav.common.types.identer.Fnr
import no.nav.poao_tilgang.client.Decision
import no.nav.poao_tilgang.client.TilgangType
import no.nav.veilarboppfolging.controller.graphql.AdGruppeNavn
import no.nav.veilarboppfolging.controller.graphql.DecisionDenyReason
import no.nav.veilarboppfolging.service.AuthService
import org.slf4j.LoggerFactory
import java.util.UUID

enum class TilgangResultat {
    HAR_TILGANG,
    IKKE_TILGANG_FORTROLIG_ADRESSE,
    IKKE_TILGANG_STRENGT_FORTROLIG_ADRESSE,
    IKKE_TILGANG_EGNE_ANSATTE,
    IKKE_TILGANG_ENHET,
    IKKE_TILGANG_MODIA
}

fun evaluerNavAnsattTilgangTilBruker(authService: AuthService, fnr: Fnr, veilederUUID: UUID) : TilgangResultat {
    authService.
}

fun evaluerNavAnsattTilgangTilEksternBruker(authService: AuthService, fnr: String): TilgangResultat {
    val decision = authService.evaluerNavAnsattTilgangTilBruker(Fnr.of(fnr), TilgangType.LESE)
    return when (decision) {
        is Decision.Deny -> decision.tryToFindDenyReason()
        Decision.Permit -> TilgangResultat.HAR_TILGANG
    }
}

private val logger = LoggerFactory.getLogger(Decision::class.java)
fun Decision.Deny.tryToFindDenyReason(): TilgangResultat {
    val denyReason = runCatching {
        DecisionDenyReason.valueOf(this.reason)
    }.getOrNull()

    return when (denyReason) {
        DecisionDenyReason.POLICY_IKKE_IMPLEMENTERT,
        DecisionDenyReason.EKSTERN_BRUKER_HAR_IKKE_TILGANG,
        DecisionDenyReason.UKLAR_TILGANG_MANGLENDE_INFORMASJON -> TilgangResultat.IKKE_TILGANG_ENHET.also {
            logger.warn("Fikk uforventet svar om ikke tilgang, skal egentlig ikke skje. Svak programmering – vi må ha gjort noe feil: ${this.reason}, ${this.message}")
        }
        DecisionDenyReason.IKKE_TILGANG_TIL_NAV_ENHET -> TilgangResultat.IKKE_TILGANG_ENHET
        DecisionDenyReason.IKKE_TILGANG_TIL_FORTROLIG_BRUKER ->  TilgangResultat.IKKE_TILGANG_FORTROLIG_ADRESSE
        DecisionDenyReason.IKKE_TILGANG_TIL_STRENGT_FORTROLIG_BRUKER,
        DecisionDenyReason.IKKE_TILGANG_TIL_STRENGT_FORTROLIG_UTLAND_BRUKER-> TilgangResultat.IKKE_TILGANG_STRENGT_FORTROLIG_ADRESSE
        DecisionDenyReason.IKKE_TILGANG_TIL_SKJERMET_PERSON -> TilgangResultat.IKKE_TILGANG_EGNE_ANSATTE
        DecisionDenyReason.MANGLER_TILGANG_TIL_AD_GRUPPE, null -> when {
            this.message.contains(AdGruppeNavn.STRENGT_FORTROLIG_ADRESSE) -> TilgangResultat.IKKE_TILGANG_STRENGT_FORTROLIG_ADRESSE
            this.message.contains(AdGruppeNavn.FORTROLIG_ADRESSE) -> TilgangResultat.IKKE_TILGANG_FORTROLIG_ADRESSE
            this.message.contains(AdGruppeNavn.EGNE_ANSATTE) -> TilgangResultat.IKKE_TILGANG_EGNE_ANSATTE
            this.message.contains(AdGruppeNavn.MODIA_GENERELL) -> TilgangResultat.IKKE_TILGANG_MODIA
            this.message.contains(AdGruppeNavn.MODIA_OPPFOLGING) -> TilgangResultat.IKKE_TILGANG_MODIA
            else -> TilgangResultat.IKKE_TILGANG_ENHET
        }
    }
}