package no.nav.veilarboppfolging.oppfolgingsbruker.kanStarteOppfolging

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class KanStarteOppfolgingTest {

    @Test
    fun `skal svare ALLEREDE_UNDER_OPPFOLGING før andre feil`() {
        val oppfolgingStatus = ALLEREDE_UNDER_OPPFOLGING
        val harTilgang = lazy { IKKE_TILGANG_FORTROLIG_ADRESSE }
        val folkeregisterStatus = lazy { IKKE_LOVLIG_OPPHOLD }
        val result = oppfolgingStatus and harTilgang and folkeregisterStatus
        assertEquals(result, KanStarteOppfolging.ALLEREDE_UNDER_OPPFOLGING)
    }

    @Test
    fun `skal svare IKKE_TILGANG_FORTROLIG_ADRESSE før andre feil`() {
        val oppfolgingStatus = KanStarteOppfolging.JA
        val harTilgang = lazy { IKKE_TILGANG_FORTROLIG_ADRESSE }
        val folkeregisterStatus = lazy { IKKE_LOVLIG_OPPHOLD }
        assertEquals(oppfolgingStatus and harTilgang and folkeregisterStatus, KanStarteOppfolging.IKKE_TILGANG_FORTROLIG_ADRESSE)
    }

    @Test
    fun `skal svare IKKE_LOVLIG_OPPHOLD når alt annet er feil`() {
        val oppfolgingStatus = OPPFOLGING_OK
        val harTilgang = lazy { TILGANG_OK }
        val folkeregisterStatus = lazy { DOD }
        assertEquals(oppfolgingStatus and harTilgang and folkeregisterStatus, KanStarteOppfolging.DOD)
    }

}