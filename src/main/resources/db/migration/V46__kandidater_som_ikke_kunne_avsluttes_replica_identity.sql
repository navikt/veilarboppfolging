-- nødløsning slik at vi får slettet duplikate rader i tabell som er streamet til bigquery
ALTER TABLE kandidater_som_ikke_kunne_avsluttes REPLICA IDENTITY FULL;

DROP INDEX IF EXISTS unique_oppfolgingsperiode_uuid;