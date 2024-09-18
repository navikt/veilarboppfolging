drop view DVH_BRUKER_REGISTRERING;
create VIEW DVH_BRUKER_REGISTRERING AS (
  SELECT
    BRUKER_REGISTRERING_ID,
    AKTOR_ID,
    OPPRETTET_DATO,
    YRKESPRAKSIS,
    NUS_KODE,
    -1 as UTDANNING_BESTATT,
    -1 as UTDANNING_GODKJENT_NORGE
  FROM BRUKER_REGISTRERING
);