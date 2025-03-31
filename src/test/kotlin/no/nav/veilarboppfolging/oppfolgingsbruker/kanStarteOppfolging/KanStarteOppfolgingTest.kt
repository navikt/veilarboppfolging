package no.nav.veilarboppfolging.oppfolgingsbruker.kanStarteOppfolging

import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.ALLEREDE_UNDER_OPPFOLGING
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.ALLEREDE_UNDER_OPPFOLGING.oppfolgingSjekk
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.DOD
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.FREG_STATUS_KREVER_MANUELL_GODKJENNING
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.FREG_STATUS_OK
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.IKKE_LOVLIG_OPPHOLD
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.IKKE_TILGANG_ENHET
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.IKKE_TILGANG_FORTROLIG_ADRESSE
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.KanStarteOppfolgingDto
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OPPFOLGING_OK
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.TILGANG_OK
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class KanStarteOppfolgingTest {

    @Test
    fun `skal svare IKKE_TILGANG_FORTROLIG_ADRESSE før andre feil`() {
        val harTilgang = lazy { IKKE_TILGANG_FORTROLIG_ADRESSE }
        val oppfolgingStatus = lazy { ALLEREDE_UNDER_OPPFOLGING }
        val folkeregisterStatus = lazy { IKKE_LOVLIG_OPPHOLD }
        val result = oppfolgingSjekk(oppfolgingStatus, harTilgang, folkeregisterStatus)
        assertEquals(result, KanStarteOppfolgingDto.IKKE_TILGANG_FORTROLIG_ADRESSE)
    }

    @Test
    fun `skal svare IKKE_TILGANG_FORTROLIG_ADRESSE før andre feil når oppfolging er OK`() {
        val harTilgang = lazy { IKKE_TILGANG_FORTROLIG_ADRESSE }
        val oppfolgingStatus = lazy {OPPFOLGING_OK }
        val folkeregisterStatus = lazy { IKKE_LOVLIG_OPPHOLD }
        val result = oppfolgingSjekk(oppfolgingStatus, harTilgang, folkeregisterStatus)
        assertEquals(result, KanStarteOppfolgingDto.IKKE_TILGANG_FORTROLIG_ADRESSE)
    }

    @Test
    fun `skal svare DOD når alt annet er riktig`() {
        val harTilgang = lazy { TILGANG_OK }
        val oppfolgingStatus = lazy {OPPFOLGING_OK}
        val folkeregisterStatus = lazy { DOD }
        val result = oppfolgingSjekk(oppfolgingStatus, harTilgang, folkeregisterStatus)
        assertEquals(result, KanStarteOppfolgingDto.DOD)
    }

    @Test
    fun `ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT skal være en OK status`() {
        val harTilgang = lazy { TILGANG_OK }
        val oppfolgingStatus = lazy {ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT}
        val folkeregisterStatus = lazy { FREG_STATUS_OK }
        val result = oppfolgingSjekk(oppfolgingStatus, harTilgang, folkeregisterStatus)
        assertEquals(result, KanStarteOppfolgingDto.ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT)
    }

    @Test
    fun `ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT skal være en OK men skal fortsatt kreve tilgang`() {
        val harTilgang = lazy { IKKE_TILGANG_ENHET }
        val oppfolgingStatus = lazy {ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT}
        val folkeregisterStatus = lazy { FREG_STATUS_OK }
        val result = oppfolgingSjekk(oppfolgingStatus, harTilgang, folkeregisterStatus)
        assertEquals(result, KanStarteOppfolgingDto.IKKE_TILGANG_ENHET)
    }

    @Test
    fun `ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT skal være en OK men fortsatt kreve manuell godkjenning av lovlig opphold`() {
        val harTilgang = lazy { TILGANG_OK }
        val oppfolgingStatus = lazy {ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT}
        val folkeregisterStatus = lazy { FREG_STATUS_KREVER_MANUELL_GODKJENNING }
        val result = oppfolgingSjekk(oppfolgingStatus, harTilgang, folkeregisterStatus)
        assertEquals(result, KanStarteOppfolgingDto.ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT_MEN_KREVER_MANUELL_GODKJENNING)
    }

}