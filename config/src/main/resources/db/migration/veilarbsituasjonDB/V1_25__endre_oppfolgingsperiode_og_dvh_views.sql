
-- Fjerner veileder for de migrerte, åpne oppfølgingsperiodene. Veileder angir bare avsluttende veileder.
UPDATE OPPFOLGINGSPERIODE 
SET VEILEDER = null
WHERE SLUTTDATO is null;

-- Endrer navn for å tydeliggjøre at veileder og kommentar gjelder avslutning av perioden
ALTER TABLE OPPFOLGINGSPERIODE
  RENAME COLUMN VEILEDER TO AVSLUTT_VEILEDER;
ALTER TABLE OPPFOLGINGSPERIODE
  RENAME COLUMN BEGRUNNELSE TO AVSLUTT_BEGRUNNELSE;

