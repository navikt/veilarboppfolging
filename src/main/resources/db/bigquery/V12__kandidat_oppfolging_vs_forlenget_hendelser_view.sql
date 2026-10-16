CREATE OR REPLACE VIEW oppfolging_metrikker.KANDIDAT_OPPFOLGING_VS_FORLENGET_HENDELSER_VIEW AS
SELECT
  DATE(`timestamp`, 'Europe/Oslo') AS dag,
  `timestamp`,
  'Forlenget' AS hendelse_type,
  forrige_hendelse,
  oppfolgingsperiode_id
FROM oppfolging_metrikker.KANDIDAT_FORLENGET_HENDELSER
WHERE hendelse = 'FORLENGELSE_OPPRETTET'

UNION ALL

SELECT
  DATE(`timestamp`, 'Europe/Oslo'),
  `timestamp`,
  'Utmeldt',
  kandidatForUtmeldingHendelseType,
  id
FROM oppfolging_metrikker.OPPFOLGINGSPERIODE_EVENTS
WHERE automatiskAvsluttet = FALSE
  AND event = 'OPPFOLGINGSPERIODE_SLUTT'
  AND erKandidatForUtmelding = TRUE
