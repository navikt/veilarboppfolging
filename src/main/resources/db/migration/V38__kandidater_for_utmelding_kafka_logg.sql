CREATE TABLE kandidater_for_utmelding_kafka_logg (
    id uuid PRIMARY KEY,
    utmeldingshendelse_id uuid NOT NULL REFERENCES kandidater_for_utmelding_hendelser(utmeldingshendelse_id),
    publiseringstype varchar NOT NULL CHECK (publiseringstype IN ('NY_KANDIDAT', 'REPUBLISERING')),
    status varchar NOT NULL CHECK (status IN ('SENDT', 'FEILET')),
    kafka_topic varchar NOT NULL,
    kafka_partition integer,
    kafka_offset bigint,
    feilmelding varchar(1000),
    opprettet_tidspunkt timestamp DEFAULT current_timestamp NOT NULL
);

CREATE INDEX kandidater_for_utmelding_kafka_logg_hendelse_idx
    ON kandidater_for_utmelding_kafka_logg(utmeldingshendelse_id);

CREATE INDEX kandidater_for_utmelding_kafka_logg_tidspunkt_idx
    ON kandidater_for_utmelding_kafka_logg(opprettet_tidspunkt);
