CREATE TABLE kandidater_for_utmelding_kafka_logg (
    id uuid PRIMARY KEY,
    utmeldingshendelse_id uuid NOT NULL REFERENCES kandidater_for_utmelding_hendelser(utmeldingshendelse_id),
    status varchar NOT NULL,
    kafka_topic varchar NOT NULL,
    kafka_partition integer,
    kafka_offset bigint,
    feilmelding text,
    opprettet_tidspunkt timestamp DEFAULT current_timestamp NOT NULL
);

CREATE INDEX kandidater_for_utmelding_kafka_logg_hendelse_idx
    ON kandidater_for_utmelding_kafka_logg(utmeldingshendelse_id);
