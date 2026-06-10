ALTER TABLE kandidat_for_utmelding ALTER COLUMN oppfolgingsperiode_uuid SET NOT NULL;
CREATE INDEX kandidat_for_utmelding_oppfolgingsperiode_uuid_index
    ON kandidat_for_utmelding (oppfolgingsperiode_uuid);
