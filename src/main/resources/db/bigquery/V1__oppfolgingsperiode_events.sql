CREATE TABLE IF NOT EXISTS oppfolging_metrikker.OPPFOLGINGSPERIODE_EVENTS (
    id                        STRING,
    event                     STRING    NOT NULL,
    timestamp                 TIMESTAMP NOT NULL,
    startBegrunnelse          STRING,
    startedAvType             STRING,
    kvalifiseringsgruppe      STRING,
    manuellSjekkLovligOpphold BOOL,
    automatiskAvsluttet       BOOL,
    avregistreringsType       STRING
)
