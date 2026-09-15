package no.nav.veilarboppfolging.kandidatForUtmelding.dto

data class UtmeldingskandidatDto(
    val tag: KandidatForUtmeldingTagDto?,
    val utmeldingskandidatHendelser: List<KandidatForUtmeldingHendelseDto>?,
    val aktivForlengelse: ForlengelseDto?
)
