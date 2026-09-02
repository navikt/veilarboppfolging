package no.nav.veilarboppfolging.client.tiltakshistorikk

data class TiltakshistorikkRequest(
    val identer: List<NorskIdent>,
)

@JvmInline
value class NorskIdent(val value: String) {
    override fun toString() = "***********"
}
