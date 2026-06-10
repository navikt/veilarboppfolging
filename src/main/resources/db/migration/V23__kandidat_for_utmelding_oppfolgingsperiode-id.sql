ALTER TABLE kandidat_for_utmelding ADD COLUMN oppfolgingsperiode_uuid char(36);
ALTER TABLE kandidat_for_utmelding ADD CONSTRAINT OPPFOLGINGSPERIODE_UUID_FK FOREIGN KEY (oppfolgingsperiode_uuid) REFERENCES OPPFOLGINGSPERIODE (uuid);

