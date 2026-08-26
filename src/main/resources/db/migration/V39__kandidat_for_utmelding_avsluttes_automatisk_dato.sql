ALTER TABLE kandidater_for_utmelding ADD COLUMN avsluttes_automatisk_dato timestamp;

-- Støtter fremtidig spørring etter kandidater som skal avsluttes automatisk
CREATE INDEX kandidat_avsluttes_automatisk_dato_idx ON kandidater_for_utmelding(avsluttes_automatisk_dato);
