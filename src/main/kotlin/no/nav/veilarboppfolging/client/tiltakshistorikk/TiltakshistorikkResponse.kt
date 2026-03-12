package no.nav.veilarboppfolging.client.tiltakshistorikk

data class TiltakshistorikkResponse(
    val historikk: List<TiltakshistorikkV1Dto>,
    val meldinger: Set<TiltakshistorikkMelding>,
) {
    fun kunneIkkeHenteDeltakelserFraTeamTiltak(): Boolean {
        return meldinger.contains(TiltakshistorikkMelding.MANGLER_HISTORIKK_FRA_TEAM_TILTAK)
    }
}

enum class TiltakshistorikkMelding {
    MANGLER_HISTORIKK_FRA_TEAM_TILTAK,
}
