CREATE TABLE IF NOT EXISTS oppfolging_metrikker.UTMELDING_COUNTS (
    personerIUtmelding                   INT64     NOT NULL,
    personIUtmeldingSomErUnderOppfolging INT64     NOT NULL,
    timestamp                            TIMESTAMP NOT NULL
)
