package no.nav.veilarboppfolging.client.norg

import no.nav.common.types.identer.EnhetId
import no.nav.veilarboppfolging.client.pdl.GeografiskTilknytningNr

interface NorgClient {
    /**
     * Henter enheten som tilhører et geografisk område
     * @param geografiskTilknytning Geografisk identifikator, kommune eller bydel, for NAV kontoret (f.eks NAV Frogner tilhører 030105)
     * @return NAV enhet som tilhører det geografiske området
     */
    fun hentTilhorendeEnhet(geografiskTilknytning: GeografiskTilknytningNr): EnhetId?
}
