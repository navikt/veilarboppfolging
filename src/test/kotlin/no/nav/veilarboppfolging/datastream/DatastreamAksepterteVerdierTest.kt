package no.nav.veilarboppfolging.datastream

import no.nav.pto_schema.enums.arena.Formidlingsgruppe
import no.nav.pto_schema.enums.arena.Hovedmaal
import no.nav.pto_schema.enums.arena.Kvalifiseringsgruppe
import no.nav.veilarboppfolging.oppfolgingsbruker.StartetAvType
import no.nav.veilarboppfolging.oppfolgingsbruker.inngang.OppfolgingStartBegrunnelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DatastreamAksepterteVerdierTest {

    private fun snapshotFor(tabellNavn: String, kolonneNavn: String): Set<String> =
        DatastreamKontrakt.tabeller
            .first { it.navn == tabellNavn }
            .kolonner
            .first { it.navn == kolonneNavn }
            .aksepterteVerdier!!

    private fun verifiserSnapshot(tabellNavn: String, kolonneNavn: String, faktiskeVerdier: Set<String>) {
        val snapshot = snapshotFor(tabellNavn, kolonneNavn)
        assertThat(snapshot)
            .withFailMessage(
                """
                Aksepterte verdier for '$tabellNavn.$kolonneNavn' har endret seg siden kontrakten ble sist oppdatert.
                Nye verdier (ikke i snapshot): ${faktiskeVerdier - snapshot}
                Fjernede verdier (ikke lenger i kode): ${snapshot - faktiskeVerdier}
                → Oppdater DatastreamKontrakt.kt og varsle datavarehus-teamet om endringen.
                """.trimIndent()
            )
            .isEqualTo(faktiskeVerdier)
    }

    // --- pto-schema-enums (ekstern avhengighet) ---
    // Disse testene feiler dersom pto-schema-biblioteket oppdateres med nye eller fjernede enum-verdier.

    @Test
    fun `formidlingsgruppe-snapshot matcher Formidlingsgruppe-enum i pto-schema`() {
        verifiserSnapshot(
            tabellNavn = "oppfolgingstatus",
            kolonneNavn = "formidlingsgruppe",
            faktiskeVerdier = Formidlingsgruppe.values().map { it.name }.toSet(),
        )
    }

    @Test
    fun `kvalifiseringsgruppe-snapshot matcher Kvalifiseringsgruppe-enum i pto-schema`() {
        verifiserSnapshot(
            tabellNavn = "oppfolgingstatus",
            kolonneNavn = "kvalifiseringsgruppe",
            faktiskeVerdier = Kvalifiseringsgruppe.values().map { it.name }.toSet(),
        )
    }

    @Test
    fun `hovedmaal-snapshot matcher Hovedmaal-enum i pto-schema`() {
        verifiserSnapshot(
            tabellNavn = "oppfolgingstatus",
            kolonneNavn = "hovedmaal",
            faktiskeVerdier = Hovedmaal.values().map { it.name }.toSet(),
        )
    }

    // --- Lokale enums (vi kontrollerer koden) ---
    // Disse testene feiler dersom noen legger til eller fjerner en enum-verdi i koden
    // uten å oppdatere kontrakten og varsle datavarehus-teamet.

    @Test
    fun `start_begrunnelse-snapshot matcher OppfolgingStartBegrunnelse-enum`() {
        verifiserSnapshot(
            tabellNavn = "oppfolgingsperiode",
            kolonneNavn = "start_begrunnelse",
            faktiskeVerdier = OppfolgingStartBegrunnelse.values().map { it.name }.toSet(),
        )
    }

    @Test
    fun `startet_av_type-snapshot matcher StartetAvType-enum`() {
        verifiserSnapshot(
            tabellNavn = "oppfolgingsperiode",
            kolonneNavn = "startet_av_type",
            faktiskeVerdier = StartetAvType.values().map { it.name }.toSet(),
        )
    }
}
