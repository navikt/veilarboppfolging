package no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse

import no.nav.common.types.identer.NorskIdent
import java.net.URL
import java.time.ZonedDateTime

data class FilterhendelseRecord(
    val personID: NorskIdent,
    val avsender: String,
    val kategori: Kategori,
    val operasjon: Operasjon,
    val hendelse: HendelseInnhold
) {
    data class HendelseInnhold(
        val beskrivelse: String,
        val dato: ZonedDateTime,
        val lenke: URL,
        val detaljer: String?,
        val utmeldingsKandidatDetaljer: UtmeldingsKandidatDetaljer?,
    )
}

enum class UtmeldingsKandidatDetaljer {
    ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE,
    ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
    ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET
}

enum class Kategori {
    UTGATT_VARSEL,
    UDELT_SAMTALEREFERAT,
    KANDIDAT_FOR_UTMELDING
}

enum class Operasjon {
    START,
    STOPP,
    OPPDATER
}
