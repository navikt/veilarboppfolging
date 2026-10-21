package no.nav.veilarboppfolging.oppfolgingsbruker

import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.veilarboppfolging.config.ApplicationConfig.ADMIN_USER_NAME
import no.nav.veilarboppfolging.config.ApplicationConfig.SYSTEM_USER_NAME

/* a person who registers something. */
sealed class Registrant() {
    abstract fun getIdent(): String
    abstract fun getType(): RegistrantType
}
class BrukerRegistrant(
    val fnr: Fnr,
) : Registrant() {
    override fun getIdent(): String = fnr.get()
    override fun getType() = RegistrantType.BRUKER
}

class SystemRegistrant(
    val systemnavn: String = SYSTEM_USER_NAME
) : Registrant() {
    override fun getIdent(): String = systemnavn
    override fun getType() = RegistrantType.SYSTEM
}

class AdminRegistrant(
    val navn: String = ADMIN_USER_NAME
) : Registrant() {
    override fun getIdent(): String = navn
    override fun getType() = RegistrantType.ADMIN
}

class VeilederRegistrant(
    val navIdent: NavIdent,
) : Registrant() {
    override fun getIdent(): String = navIdent.get()
    override fun getType() = RegistrantType.VEILEDER
}

fun RegistrantType.toRegistrant(navIdent: NavIdent, fnr: Fnr, systemnavn: String = SYSTEM_USER_NAME): Registrant {
    return when (this) {
        RegistrantType.SYSTEM -> SystemRegistrant(systemnavn)
        RegistrantType.BRUKER -> BrukerRegistrant(fnr)
        RegistrantType.VEILEDER -> VeilederRegistrant(navIdent)
        RegistrantType.ADMIN -> AdminRegistrant()
    }
}