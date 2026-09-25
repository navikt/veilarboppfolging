CREATE TABLE IF NOT EXISTS oppfolging_metrikker.KANDIDAT_FORLENGET_HENDELSER(
    hendelse                  STRING NOT NULL,
    forlenget_til             DATE NOT NULL,
    oppfolgingsperiode_id     STRING NOT NULL,
    hendelse_opprettet        TIMESTAMP NOT NULL,
    timestamp                 TIMESTAMP NOT NULL
);