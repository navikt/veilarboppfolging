package no.nav.veilarboppfolging.oppfolgingsbruker

import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NavIdent
import no.nav.veilarboppfolging.config.ApplicationConfig.SYSTEM_USER_NAME
import no.nav.veilarboppfolging.domain.StartetAvType

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

object SystemRegistrant : Registrant() {
    override fun getIdent(): String = SYSTEM_REGISTRANT_NAME
    override fun getType() = StartetAvType.SYSTEM

    const val SYSTEM_REGISTRANT_NAME = SYSTEM_USER_NAME
}

class VeilederRegistrant(
    val navIdent: NavIdent,
) : Registrant() {
    override fun getIdent(): String = navIdent.get()
    override fun getType() = StartetAvType.VEILEDER
}

fun StartetAvType.toRegistrant(navIdent: NavIdent, fnr: Fnr): Registrant {
    return when (this) {
        StartetAvType.SYSTEM -> SystemRegistrant
        StartetAvType.BRUKER -> BrukerRegistrant(fnr)
        StartetAvType.VEILEDER -> VeilederRegistrant(navIdent)
    }
}