ALTER TABLE kandidater_for_utmelding_hendelser ADD COLUMN hendelse_tidspunkt timestamp;

UPDATE kandidater_for_utmelding_hendelser SET hendelse_tidspunkt = created_at;

ALTER TABLE kandidater_for_utmelding_hendelser ALTER COLUMN hendelse_tidspunkt SET NOT NULL;
