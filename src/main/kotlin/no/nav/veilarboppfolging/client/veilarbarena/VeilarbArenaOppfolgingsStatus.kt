package no.nav.veilarboppfolging.client.veilarbarena

import java.time.LocalDate


/**
 * Har ikke feltene "hovedmaal" og "kvalifiseringsgruppe" (men servicegruppe er egentlig kvalifiseringsgruppe) som [VeilarbArenaOppfolgingsBruker] har
 * @see VeilarbArenaOppfolgingsBruker
 */
data class VeilarbArenaOppfolgingsStatus(
    val rettighetsgruppe: String?,
    val formidlingsgruppe: String?,
    val servicegruppe: String?,
    val oppfolgingsenhet: String?,
    val inaktiveringsdato: LocalDate? = null,
    val kanEnkeltReaktiveres: Boolean? = null,
)
