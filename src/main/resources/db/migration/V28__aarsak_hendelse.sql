/*
[Bekreftelse] Ønsket ikke lenger å være arbeidssøker
Stopp av periode
[Bekreftelse] ikke levert innen fristen
[Bekreftelse:ytelse/støtte] Ikke levert innen fristen
Feilregistrering
*/

update kandidat_for_utmelding
set hendelse = 'ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE'
where hendelse = 'ARBEIDSSOKERPERIODE_AVSLUTTET'
    and aarsak = '[Bekreftelse] Ønsket ikke lenger å være arbeidssøker';
