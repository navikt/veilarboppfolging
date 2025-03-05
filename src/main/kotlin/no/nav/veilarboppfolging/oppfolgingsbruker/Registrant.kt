package no.nav.veilarboppfolging.oppfolgingsbruker

import no.nav.common.types.identer.NavIdent
import no.nav.veilarboppfolging.config.ApplicationConfig.SYSTEM_USER_NAME
import no.nav.veilarboppfolging.domain.StartetAvType

interface RegistrantFelter {
    /**
     * - Gir null hvis registrert av bruker
     * - Gir en stringen "System" hvis registrert av system
     * - Gir velederIdent hvis registrert av veileder
     */
    fun getRegistrertAv(): String?
    fun getRegistrertAvType(): StartetAvType
}

/* a person who registers something. */
sealed class Registrant: RegistrantFelter

object BrukerRegistrant : Registrant() {
    override fun getRegistrertAv(): String? = null
    override fun getRegistrertAvType() = StartetAvType.BRUKER
}

object SystemRegistrant : Registrant() {
    override fun getRegistrertAv(): String? = SYSTEM_REGISTRANT_NAME
    override fun getRegistrertAvType() = StartetAvType.SYSTEM

    const val SYSTEM_REGISTRANT_NAME = SYSTEM_USER_NAME
}

class VeilederRegistrant(
    val navIdent: NavIdent,
) : Registrant() {
    override fun getRegistrertAv(): String? = navIdent.get()
    override fun getRegistrertAvType() = StartetAvType.VEILEDER
}

fun StartetAvType.toRegistrant(navIdent: NavIdent): Registrant {
    return when (this) {
        StartetAvType.SYSTEM -> SystemRegistrant
        StartetAvType.BRUKER -> BrukerRegistrant
        StartetAvType.VEILEDER -> VeilederRegistrant(navIdent)
    }
}