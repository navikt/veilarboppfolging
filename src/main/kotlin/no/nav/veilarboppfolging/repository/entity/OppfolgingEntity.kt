package no.nav.veilarboppfolging.repository.entity

import no.nav.common.types.identer.EnhetId
import no.nav.veilarboppfolging.`14a`.Innsatsgruppe
import no.nav.veilarboppfolging.oppfolgingsbruker.arena.LocalArenaOppfolging
import java.util.*

/**
 * The OppfolgingTable class is used as a transient data carrier from the
 * database layer (OppfolgingsStatusRepository) to the service layer.
 * Please avoid downstream use of this class. Instead, try to use
 * [no.nav.veilarboppfolging.domain.Oppfolging] and related methods.
 */
data class OppfolgingEntity(
    val aktorId: String?,
    val veilederId: String?,
    val underOppfolging: Boolean,
    val gjeldendeManuellStatusId: Long?,
    val gjeldendeMaalId: Long?,
    val gjeldendeKvpId: Long?,
    val oppfolgingsEnhet: EnhetId?,
    // Kan ofte være null, ikke alle brukere har 14a-vedtak som gir dem en innsatsgruppe
    val innsatsgruppe: Innsatsgruppe?,
    val localArenaOppfolging: Optional<LocalArenaOppfolging>,
)
