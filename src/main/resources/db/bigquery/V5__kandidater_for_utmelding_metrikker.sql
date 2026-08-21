CREATE TABLE IF NOT EXISTS oppfolging_metrikker.KANDIDATER_FOR_UTMELDING_METRIKKER (
    antallKandidaterForUtmelding           INT64     NOT NULL,
    antallUnderOppfolgingMedIserv          INT64     NOT NULL,
    antallKandidaterForUtmeldingForlenget  INT64     NOT NULL,
    timestamp                               TIMESTAMP NOT NULL
)
