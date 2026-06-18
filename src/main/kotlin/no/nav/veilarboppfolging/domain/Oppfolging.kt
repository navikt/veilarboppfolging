package no.nav.veilarboppfolging.domain

import no.nav.veilarboppfolging.repository.entity.KvpPeriodeEntity
import no.nav.veilarboppfolging.repository.entity.MaalEntity
import no.nav.veilarboppfolging.repository.entity.ManuellStatusEntity
import no.nav.veilarboppfolging.repository.entity.OppfolgingsperiodeEntity

data class Oppfolging(
    val aktorId: String,
    val veilederId: String?,
    val underOppfolging: Boolean,
    val gjeldendeManuellStatus: ManuellStatusEntity?,
    val gjeldendeMal: MaalEntity?,
    val oppfolgingsperioder: List<OppfolgingsperiodeEntity>,
    val gjeldendeKvp: KvpPeriodeEntity?,
)
