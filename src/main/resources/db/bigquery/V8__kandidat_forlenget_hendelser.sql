CREATE TABLE IF NOT EXISTS oppfolging_metrikker.kandidat_forlenget_hendelser(
    utmeldinghendelse_id      STRING NOT NULL PRIMARY KEY,
    hendelse                  STRING NOT NULL,
    forlenget_til             TIMESTAMP NOT NULL,
    oppfolgingsperiode_id     STRING NOT NULL,
    hendelse_opprettet        TIMESTAMP NOT NULL,
    timestamp                 TIMESTAMP NOT NULL,
);