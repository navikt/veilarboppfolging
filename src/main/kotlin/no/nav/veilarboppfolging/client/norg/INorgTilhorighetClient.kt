package no.nav.veilarboppfolging.client.norg

interface INorgTilhorighetClient {
    /**
     * Henter enheten som tilhører et geografisk område
     * @param geografiskTilknytning Geografisk identifikator, kommune eller bydel, for NAV kontoret (f.eks NAV Frogner tilhører 030105)
     * @param skjermet Personen er egen ansatt
     * @param fortroligAdresse Personen har fortrolig adresse
     * @return NAV enhet som tilhører det geografiske området
     */
    fun hentTilhorendeEnhet(norgTilhorighetRequest: NorgTilhorighetRequest): Enhet?
}
