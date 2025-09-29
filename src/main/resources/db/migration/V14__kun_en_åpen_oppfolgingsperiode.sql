-- UNIQUE(aktor_id, slutt_dato) ville tillatt flere NULL-verdier (NULL != NULL), så bruker
-- partial unique index for å sikre maks én rad uten slutt_dato per aktor_id.
CREATE UNIQUE INDEX unique_aapen_periode
    ON oppfolgingsperiode (aktor_id)
    WHERE sluttdato IS NULL;