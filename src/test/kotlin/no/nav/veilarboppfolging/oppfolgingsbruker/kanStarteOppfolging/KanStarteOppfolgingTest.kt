package no.nav.veilarboppfolging.oppfolgingsbruker.kanStarteOppfolging

import no.nav.veilarboppfolging.client.pdl.ForenkletFolkeregisterStatus
import no.nav.veilarboppfolging.client.pdl.FregStatusOgStatsborgerskap
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.ALLEREDE_UNDER_OPPFOLGING
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.DOD
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_DNUMMER_IKKE_EOS
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_IKKE_BOSATT
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.FREG_STATUS_OK
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.IKKE_LOVLIG_OPPHOLD
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.IKKE_TILGANG_ENHET
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.IKKE_TILGANG_FORTROLIG_ADRESSE
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.KanStarteOppfolgingDto
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.KanStarteOppfolgingSjekk.Companion.sjekkKanStarteOppfolgingPaBrukerForVeileder
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OPPFOLGING_OK
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.TILGANG_OK
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.toKanStarteOppfolging
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class KanStarteOppfolgingTest {

    @Test
    fun `skal svare IKKE_TILGANG_FORTROLIG_ADRESSE før andre feil`() {
        val harTilgang = lazy { IKKE_TILGANG_FORTROLIG_ADRESSE }
        val oppfolgingStatus = lazy { ALLEREDE_UNDER_OPPFOLGING }
        val folkeregisterStatus = lazy { IKKE_LOVLIG_OPPHOLD }
        val brukerUnder18 = lazy { true }
        val result = sjekkKanStarteOppfolgingPaBrukerForVeileder(brukerUnder18, oppfolgingStatus,harTilgang, folkeregisterStatus)
        assertEquals(result, KanStarteOppfolgingDto.IKKE_TILGANG_FORTROLIG_ADRESSE)
    }

    @Test
    fun `skal svare IKKE_TILGANG_FORTROLIG_ADRESSE før andre feil når oppfolging er OK`() {
        val harTilgang = lazy { IKKE_TILGANG_FORTROLIG_ADRESSE }
        val oppfolgingStatus = lazy { OPPFOLGING_OK }
        val folkeregisterStatus = lazy { IKKE_LOVLIG_OPPHOLD }
        val brukerUnder18 = lazy { true }
        val result = sjekkKanStarteOppfolgingPaBrukerForVeileder(brukerUnder18, oppfolgingStatus, harTilgang, folkeregisterStatus)
        assertEquals(result, KanStarteOppfolgingDto.IKKE_TILGANG_FORTROLIG_ADRESSE)
    }

    @Test
    fun `Skal svare DOD når alle andre sjekker er ok`() {
        val harTilgang = lazy { TILGANG_OK }
        val oppfolgingStatus = lazy { OPPFOLGING_OK }
        val folkeregisterStatus = lazy { DOD }
        val brukerUnder18 = lazy { true }
        val result = sjekkKanStarteOppfolgingPaBrukerForVeileder(brukerUnder18,oppfolgingStatus, harTilgang, folkeregisterStatus)
        assertEquals(result, KanStarteOppfolgingDto.DOD)
    }

    @Test
    fun `ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT skal være en ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT`() {
        val harTilgang = lazy { TILGANG_OK }
        val oppfolgingStatus = lazy { ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT }
        val folkeregisterStatus = lazy { FREG_STATUS_OK }
        val brukerUnder18 = lazy { true }
        val result = sjekkKanStarteOppfolgingPaBrukerForVeileder(brukerUnder18, oppfolgingStatus, harTilgang, folkeregisterStatus)
        assertEquals(result, KanStarteOppfolgingDto.ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT)
    }

    @Test
    fun `ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT skal være en OK men skal fortsatt kreve tilgang`() {
        val harTilgang = lazy { IKKE_TILGANG_ENHET }
        val oppfolgingStatus = lazy { ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT }
        val folkeregisterStatus = lazy { FREG_STATUS_OK }
        val brukerUnder18 = lazy { true }
        val result = sjekkKanStarteOppfolgingPaBrukerForVeileder(brukerUnder18, oppfolgingStatus, harTilgang, folkeregisterStatus)
        assertEquals(result, KanStarteOppfolgingDto.IKKE_TILGANG_ENHET)
    }

    @Test
    fun `ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT + ikkeBosatt skal være en OK men fortsatt kreve manuell godkjenning av lovlig opphold`() {
        val harTilgang = lazy { TILGANG_OK }
        val oppfolgingStatus = lazy { ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT }
        val folkeregisterStatus = lazy { FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_IKKE_BOSATT }
        val brukerUnder18 = lazy { true }
        val result = sjekkKanStarteOppfolgingPaBrukerForVeileder(brukerUnder18, oppfolgingStatus, harTilgang, folkeregisterStatus)
        assertEquals(
            result,
            KanStarteOppfolgingDto.ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT_MEN_KREVER_MANUELL_GODKJENNING_PGA_IKKE_BOSATT
        )
    }

    @Test
    fun `ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT + dnummer og ikke eu eller eos skal være en OK men fortsatt kreve manuell godkjenning av lovlig opphold`() {
        val harTilgang = lazy { TILGANG_OK }
        val oppfolgingStatus = lazy { ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT }
        val folkeregisterStatus = lazy { FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_DNUMMER_IKKE_EOS }
        val brukerUnder18 = lazy { true }
        val result = sjekkKanStarteOppfolgingPaBrukerForVeileder(brukerUnder18, oppfolgingStatus, harTilgang, folkeregisterStatus)
        assertEquals(
            result,
            KanStarteOppfolgingDto.ALLEREDE_UNDER_OPPFOLGING_MEN_INAKTIVERT_MEN_KREVER_MANUELL_GODKJENNING_PGA_DNUMMER_IKKE_EOS
        )
    }

    @Test
    fun `FregStatusOgStatsborgerskap - FREG status bosatt + NOR er OK`() {
        val norsk = FregStatusOgStatsborgerskap(
            fregStatus = ForenkletFolkeregisterStatus.bosattEtterFolkeregisterloven,
            statsborgerskap = listOf("NOR"),
            under18 = false,
        )
        assertEquals(norsk.toKanStarteOppfolging(), FREG_STATUS_OK)
    }

    @Test
    fun `FregStatusOgStatsborgerskap - FREG dNummer + GBR er OK`() {
        val gbr = FregStatusOgStatsborgerskap(
            fregStatus = ForenkletFolkeregisterStatus.dNummer,
            statsborgerskap = listOf("GBR"),
            under18 = false,
        )
        assertEquals(gbr.toKanStarteOppfolging(), FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_DNUMMER_IKKE_EOS)
    }

    @Test
    fun `FregStatusOgStatsborgerskap - FREG ikkeBosatt + GBR skal kreve manuell godkjenning`() {
        val gbr = FregStatusOgStatsborgerskap(
            fregStatus = ForenkletFolkeregisterStatus.ikkeBosatt,
            statsborgerskap = listOf("GBR"),
            under18 = false,
        )
        assertEquals(gbr.toKanStarteOppfolging(), FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_IKKE_BOSATT)
    }

    @Test
    fun `FregStatusOgStatsborgerskap - FREG dNummer + POL er OK`() {
        val eueos = FregStatusOgStatsborgerskap(
            fregStatus = ForenkletFolkeregisterStatus.dNummer,
            statsborgerskap = listOf("POL"),
            under18 = false,
        )
        assertEquals(eueos.toKanStarteOppfolging(), FREG_STATUS_OK)
    }

    @Test
    fun `FregStatusOgStatsborgerskap - FREG utflyttet (forenklet status ikkeBosatt) + POL er OK`() {
        val eueos = FregStatusOgStatsborgerskap(
            fregStatus = ForenkletFolkeregisterStatus.ikkeBosatt,
            statsborgerskap = listOf("POL"),
            under18 = false,
        )
        assertEquals(eueos.toKanStarteOppfolging(), FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_IKKE_BOSATT)
    }

    @Test
    fun `FregStatusOgStatsborgerskap - FREG status dNummer (midlertidig eller inaktiv) + AFG krever manuell godkjenning`() {
        val ikkeEueos = FregStatusOgStatsborgerskap(
            fregStatus = ForenkletFolkeregisterStatus.dNummer,
            statsborgerskap = listOf("AFG"),
            under18 = false,
        )
        assertEquals(ikkeEueos.toKanStarteOppfolging(), FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_DNUMMER_IKKE_EOS)
    }

    @Test
    fun `FregStatusOgStatsborgerskap - FREG status ikkeBosatt (utflyttet, foedselsregistrert eller ikkeBosatt) + AFG krever manuell godkjenning`() {
        val ikkeEueos = FregStatusOgStatsborgerskap(
            fregStatus = ForenkletFolkeregisterStatus.ikkeBosatt,
            statsborgerskap = listOf("AFG"),
            under18 = false,
        )
        assertEquals(ikkeEueos.toKanStarteOppfolging(), FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_IKKE_BOSATT)
    }

    @Test
    fun `FregStatusOgStatsborgerskap - FREG dNummer + GHA (ikke EU eller EOS) skal kreve manuell godkjenning`() {
        val eueos = FregStatusOgStatsborgerskap(
            fregStatus = ForenkletFolkeregisterStatus.dNummer,
            statsborgerskap = listOf("GHA"),
            under18 = false,
        )
        assertEquals(eueos.toKanStarteOppfolging(), FREG_STATUS_KREVER_MANUELL_GODKJENNING_PGA_DNUMMER_IKKE_EOS)
    }
}