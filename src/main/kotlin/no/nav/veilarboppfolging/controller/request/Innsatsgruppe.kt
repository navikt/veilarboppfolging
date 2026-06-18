package no.nav.veilarboppfolging.controller.request

enum class Innsatsgruppe(innsatsgruppeKode: String) {
    STANDARD_INNSATS("IKVAL"),
    SITUASJONSBESTEMT_INNSATS("BFORM"),
    BEHOV_FOR_ARBEIDSEVNEVURDERING("BKART");

    private val kode: String?

    init {
        this.kode = innsatsgruppeKode
    }
}
