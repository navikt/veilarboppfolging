package no.nav.veilarboppfolging.datastream

/**
 * Eksplisitt kontrakt over hvilke Postgres-tabeller og -kolonner som er konfigurert
 * som Datastream-streams til BigQuery.
 *
 * Datavarehus er konsument av disse dataene og bruker dem til rapportering.
 * Denne kontrakten er kilden til sannhet for hvilke verdier datavarehuset kan forvente.
 *
 * Ved endringer i tabeller, kolonner eller aksepterte verdier:
 *   1. Oppdater denne kontrakten
 *   2. Varsle datavarehus-teamet slik at dbt-tester og rapporter kan oppdateres
 */
object DatastreamKontrakt {

    data class Kolonne(val navn: String, val aksepterteVerdier: Set<String>? = null)

    /**
     * @param kolonner Kolonner som repliseres til BigQuery.
     * @param ikkeReplikerteKolonner Kolonner som finnes i Postgres-tabellen men bevisst ikke repliseres.
     *
     * Alle kolonner på tabellen må være dekket av én av de to listene. En test verifiserer
     * dette, slik at nye kolonner ikke stille begynner å replikeres uten at noen tar stilling til det.
     */
    data class Tabell(
        val navn: String,
        val kolonner: List<Kolonne>,
        val ikkeReplikerteKolonner: Set<String> = emptySet(),
    )

    val tabeller = listOf(
        Tabell(
            navn = "manuell_status",
            kolonner = listOf(
                Kolonne("aktor_id"),
                Kolonne("manuell"),
                Kolonne("opprettet_dato"),
            ),
            ikkeReplikerteKolonner = setOf("id", "begrunnelse", "opprettet_av", "opprettet_av_brukerid"),
        ),
        Tabell(
            navn = "oppfolgingsperiode",
            kolonner = listOf(
                Kolonne("aktor_id"),
                Kolonne("avslutt_veileder"),
                Kolonne("sluttdato"),
                Kolonne("oppdatert"),
                Kolonne("startdato"),
                Kolonne("uuid"),
                Kolonne(
                    "start_begrunnelse", aksepterteVerdier = setOf(
                        "ARBEIDSSOKER_REGISTRERING",
                        "ARENA_SYNC_ARBS",
                        "ARENA_SYNC_IARBS",
                        "REAKTIVERT_OPPFØLGING",
                        "MANUELL_REGISTRERING_VEILEDER",
                    )
                ),
                Kolonne("startet_av"),
                Kolonne("startet_av_type", aksepterteVerdier = setOf("SYSTEM", "BRUKER", "VEILEDER")),
                Kolonne("kontor_satt_av_veileder"),
            ),
            ikkeReplikerteKolonner = setOf("avslutt_begrunnelse", "ao_kontor_intern_person_id"),
        ),
        Tabell(
            navn = "oppfolgingstatus",
            kolonner = listOf(
                Kolonne("aktor_id"),
                Kolonne("under_oppfolging"),
                Kolonne("veileder"),
                Kolonne("oppdatert"),
                Kolonne("innsatsgruppe", aksepterteVerdier = setOf("IKVAL", "BATT", "BFORM", "VARIG")),
                Kolonne("servicegruppe", aksepterteVerdier = setOf("VURDU", "VURDI", "OPPFI", "IVURD", "BKART")),
                Kolonne(
                    "kvalifiseringsgruppe", aksepterteVerdier = setOf(
                        "BATT", "BFORM", "BKART", "IKVAL", "IVURD", "KAP11", "OPPFI", "VARIG", "VURDI", "VURDU",
                    )
                ),
                Kolonne(
                    "formidlingsgruppe", aksepterteVerdier = setOf(
                        "ARBS", "IARBS", "IJOBS", "ISERV", "JOBBS", "PARBS", "RARBS",
                    )
                ),
                Kolonne("hovedmaal", aksepterteVerdier = setOf("BEHOLDEA", "OKEDELT", "SKAFFEA")),
                Kolonne("oppfolgingsenhet"),
                Kolonne("iserv_fra_dato"),
                Kolonne("enhet"),
            ),
            ikkeReplikerteKolonner = setOf(
                "gjeldende_manuell_status",
                "gjeldende_mal",
                "gjeldende_eskaleringsvarsel",
                "gjeldende_kvp",
                "ny_for_veileder",
                "sist_tilordnet",
            ),
        ),
    )
}
