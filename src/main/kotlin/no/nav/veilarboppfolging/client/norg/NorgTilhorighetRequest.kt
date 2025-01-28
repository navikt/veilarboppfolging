package no.nav.veilarboppfolging.client.norg

import no.nav.veilarboppfolging.client.pdl.GeografiskTilknytningNr

data class NorgTilhorighetRequest(val geografiskTilknytning: GeografiskTilknytningNr, val skjermet: Boolean, val fortroligAdresse: Boolean)