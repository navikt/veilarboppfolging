package no.nav.veilarboppfolging.datastream

import no.nav.veilarboppfolging.LocalDatabaseSingleton
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DatastreamSkjemaTest {

    private val jdbcTemplate = LocalDatabaseSingleton.jdbcTemplate

    @Test
    fun `alle datastream-tabeller eksisterer i databaseskjema`() {
        val eksisterendeTabeller = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
            String::class.java
        )

        DatastreamKontrakt.tabeller.forEach { tabell ->
            assertThat(eksisterendeTabeller)
                .withFailMessage(
                    "Datastream-tabell '${tabell.navn}' finnes ikke i databaseskjema. " +
                    "Er tabellen slettet eller omdøpt? Oppdater DatastreamKontrakt.kt og varsle datavarehus-teamet."
                )
                .contains(tabell.navn)
        }
    }

    @Test
    fun `alle kolonner på datastream-tabeller er deklarert i kontrakten`() {
        DatastreamKontrakt.tabeller.forEach { tabell ->
            val alleKolonnerIDb = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_schema = 'public' AND table_name = ?",
                String::class.java,
                tabell.navn
            ).toSet()

            val deklarerteKolonner = tabell.kolonner.map { it.navn }.toSet()
            val udeklarerteKolonner = alleKolonnerIDb - deklarerteKolonner - tabell.ikkeReplikerteKolonner

            assertThat(udeklarerteKolonner)
                .withFailMessage(
                    "Tabell '${tabell.navn}' har kolonner som ikke er deklarert i DatastreamKontrakt: $udeklarerteKolonner. " +
                    "Legg kolonnen til i 'kolonner' (repliseres) eller 'ikkeReplikerteKolonner' (repliseres ikke), " +
                    "og varsle datavarehus-teamet om nødvendig."
                )
                .isEmpty()
        }
    }

    @Test
    fun `alle replikerte kolonner eksisterer i databaseskjema`() {
        DatastreamKontrakt.tabeller.forEach { tabell ->
            val eksisterendeKolonner = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_schema = 'public' AND table_name = ?",
                String::class.java,
                tabell.navn
            )

            tabell.kolonner.forEach { kolonne ->
                assertThat(eksisterendeKolonner)
                    .withFailMessage(
                        "Kolonne '${kolonne.navn}' på Datastream-tabell '${tabell.navn}' finnes ikke i databaseskjema. " +
                        "Er kolonnen slettet eller omdøpt? Oppdater DatastreamKontrakt.kt og varsle datavarehus-teamet."
                    )
                    .contains(kolonne.navn)
            }
        }
    }
}
