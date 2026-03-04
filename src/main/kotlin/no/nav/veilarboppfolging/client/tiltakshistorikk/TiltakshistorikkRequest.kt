package no.nav.veilarboppfolging.client.tiltakshistorikk

data class TiltakshistorikkRequest(
    val identer: List<NorskIdent>,
    val maxAgeYears: Int? = null,
)

@JvmInline
value class NorskIdent(val value: String) {
    override fun toString() = "***********"
}
