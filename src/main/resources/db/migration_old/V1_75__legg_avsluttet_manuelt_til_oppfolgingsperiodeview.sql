CREATE OR REPLACE VIEW DVH_OPPFOLGINGSHISTORIKK AS (
   SELECT
       OPPDATERT,
       AKTOR_ID,
       STARTDATO,
       SLUTTDATO,
       UUID,
       -- Oppfølging avsluttet automatisk grunnet iserv i 28 dager -> "System"
       -- Oppfølging avsluttet automatisk pga. inaktiv bruker som ikke kan reaktiveres -> NULL
       CASE WHEN (avslutt_veileder = 'System' OR avslutt_veileder is null)
           THEN 0
           ELSE 1
       END as MANUELT_AVSLUTTET
   FROM OPPFOLGINGSPERIODE
);
