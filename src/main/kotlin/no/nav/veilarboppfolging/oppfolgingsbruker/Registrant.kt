package no.nav.veilarboppfolging.oppfolgingsbruker

import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.veilarboppfolging.config.ApplicationConfig.SYSTEM_USER_NAME

/* a person who registers something. */
sealed class Registrant() {
    abstract fun getIdent(): String
    abstract fun getType(): StartetAvType
}
class BrukerRegistrant(
    val fnr: Fnr,
) : Registrant() {
    override fun getIdent(): String = fnr.get()
    override fun getType() = StartetAvType.BRUKER
}

class SystemRegistrant(
    val systemnavn: String = SYSTEM_USER_NAME
) : Registrant() {
    override fun getIdent(): String = systemnavn
    override fun getType() = StartetAvType.SYSTEM
}

class VeilederRegistrant(
    val navIdent: NavIdent,
) : Registrant() {
    override fun getIdent(): String = navIdent.get()
    override fun getType() = StartetAvType.VEILEDER
}

fun StartetAvType.toRegistrant(navIdent: NavIdent, fnr: Fnr, systemnavn: String = SYSTEM_USER_NAME): Registrant {
    return when (this) {
        StartetAvType.SYSTEM -> SystemRegistrant(systemnavn)
        StartetAvType.BRUKER -> BrukerRegistrant(fnr)
        StartetAvType.VEILEDER -> VeilederRegistrant(navIdent)
    }
}