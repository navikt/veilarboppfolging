package no.nav.veilarboppfolging.oppfolgingsbruker.kanStarteOppfolging

import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.ALLEREDE_UNDER_OPPFOLGING
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
    fun `skal svare ALLEREDE_UNDER_OPPFOLGING før andre feil`() {
        val oppfolgingStatus = ALLEREDE_UNDER_OPPFOLGING
        val harTilgang = lazy { IKKE_TILGANG_FORTROLIG_ADRESSE }
        val folkeregisterStatus = lazy { IKKE_LOVLIG_OPPHOLD }
        val result = oppfolgingStatus and harTilgang and folkeregisterStatus
        assertEquals(result, KanStarteOppfolgingDto.ALLEREDE_UNDER_OPPFOLGING)
    }

    @Test
    fun `skal svare IKKE_TILGANG_FORTROLIG_ADRESSE før andre feil`() {
        val oppfolgingStatus = KanStarteOppfolgingDto.JA
        val harTilgang = lazy { IKKE_TILGANG_FORTROLIG_ADRESSE }
        val folkeregisterStatus = lazy { IKKE_LOVLIG_OPPHOLD }
        assertEquals(oppfolgingStatus and harTilgang and folkeregisterStatus, KanStarteOppfolgingDto.IKKE_TILGANG_FORTROLIG_ADRESSE)
    }

    @Test
    fun `skal svare DOD når alt annet er riktig`() {
        val oppfolgingStatus = OPPFOLGING_OK
        val harTilgang = lazy { TILGANG_OK }
        val folkeregisterStatus = lazy { DOD }
        assertEquals(oppfolgingStatus and harTilgang and folkeregisterStatus, KanStarteOppfolgingDto.DOD)
    }

    @Test
    fun `ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT skal være en OK status`() {
        val oppfolgingStatus = ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT
        val harTilgang = lazy { TILGANG_OK }
        val folkeregisterStatus = lazy { FREG_STATUS_OK }
        assertEquals(KanStarteOppfolgingDto.ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT, oppfolgingStatus and harTilgang and folkeregisterStatus)
    }

    @Test
    fun `ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT skal være en OK men skal fortsatt kreve tilgang`() {
        val oppfolgingStatus = ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT
        val harTilgang = lazy { IKKE_TILGANG_ENHET }
        val folkeregisterStatus = lazy { FREG_STATUS_OK }
        assertEquals(oppfolgingStatus and harTilgang and folkeregisterStatus, KanStarteOppfolgingDto.ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT_MEN_KREVER_MANUELL_GODKJENNING)
    }

    @Test
    fun `ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT skal være en OK men fortsatt kreve manuell godkjenning av lovlig opphold`() {
        val oppfolgingStatus = ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT
        val harTilgang = lazy { TILGANG_OK }
        val folkeregisterStatus = lazy { FREG_STATUS_KREVER_MANUELL_GODKJENNING }
        assertEquals(KanStarteOppfolgingDto.ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT_MEN_KREVER_MANUELL_GODKJENNING, oppfolgingStatus and harTilgang and folkeregisterStatus)
    }

}