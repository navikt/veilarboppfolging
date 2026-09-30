-- dette er nødvendig fordi vi replikerer denne tabellen til bigquery via datastream, og for at den skal få med seg updates og deletes må vi angi noe den kan bruke for å vite hvilken rad som er endret
CREATE UNIQUE INDEX unique_oppfolgingsperiode_uuid ON kandidater_som_ikke_kunne_avsluttes(oppfolgingsperiode_uuid);

ALTER TABLE kandidater_som_ikke_kunne_avsluttes REPLICA IDENTITY USING INDEX unique_oppfolgingsperiode_uuid;