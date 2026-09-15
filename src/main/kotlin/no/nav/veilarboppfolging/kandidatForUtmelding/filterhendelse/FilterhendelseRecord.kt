package no.nav.veilarboppfolging.kandidatForUtmelding.filterhendelse

import no.nav.common.types.identer.NorskIdent
import java.net.URL
import java.time.ZonedDateTime

data class FilterhendelseRecord(
    val personID: NorskIdent,
    val avsender: String = "veilarboppfolging",
    val kategori: Kategori,
    val operasjon: Operasjon,
    val hendelse: HendelseInnhold?,
    val schemaVersjon: String = "V2",
) {
    data class HendelseInnhold(
        val beskrivelse: String,
        val beskrivelseEnum: String?,
        val tidspunkt: ZonedDateTime,
        val lenke: URL,
        val detaljer: String?,
        val tidspunktFrist: ZonedDateTime?,
    )
}

enum class BeskrivelseEnum {
    ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE,
    ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT,
    ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET,
    FORLENGELSE_UTLOPT,
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
