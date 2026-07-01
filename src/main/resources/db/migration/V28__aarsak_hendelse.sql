/*
[Bekreftelse] Ønsket ikke lenger å være arbeidssøker
Stopp av periode
[Bekreftelse] ikke levert innen fristen
[Bekreftelse:ytelse/støtte] Ikke levert innen fristen
Feilregistrering
*/
update kandidat_for_utmelding
set hendelse = 'ARBEIDSSOKERPERIODE_AVSLUTTET_SVARTE_NEI_I_BEKREFTELSE', detaljer = aarsak
where hendelse = 'ARBEIDSSOKERPERIODE_AVSLUTTET'
    and aarsak = '[Bekreftelse] Ønsket ikke lenger å være arbeidssøker';

update kandidat_for_utmelding
set hendelse = 'ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT', detaljer = aarsak
where hendelse = 'ARBEIDSSOKERPERIODE_AVSLUTTET'
    and aarsak = '[Bekreftelse] ikke levert innen fristen' OR aarsak = '[Bekreftelse:ytelse/støtte] Ikke levert innen fristen';

update kandidat_for_utmelding
set hendelse = 'ARBEIDSSOKERPERIODE_AVSLUTTET_ANNET', detaljer = aarsak
where hendelse = 'ARBEIDSSOKERPERIODE_AVSLUTTET'
    and aarsak = 'Stopp av periode' OR aarsak = 'Feilregistrering';
