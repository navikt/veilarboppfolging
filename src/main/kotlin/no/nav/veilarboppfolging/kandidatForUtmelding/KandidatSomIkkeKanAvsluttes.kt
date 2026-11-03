package no.nav.veilarboppfolging.kandidatForUtmelding

import java.util.UUID

data class KandidatSomIkkeKanAvsluttes(
    val oppfolgingsperiodeId: UUID,
    val sisteUtmeldingshendelseId: UUID,
    val begrunnelse: String?,
)
