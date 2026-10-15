package no.nav.veilarboppfolging.arbeidssokerperiode

import java.time.Instant
import no.nav.common.types.identer.AktorId
import no.nav.common.types.identer.Fnr

data class AktivArbeidssokerperiode(
    val arbeidssokerperiodeId: String,
    val aktorId: AktorId,
    val fnr: Fnr,
    val startetTidspunkt: Instant,
)
