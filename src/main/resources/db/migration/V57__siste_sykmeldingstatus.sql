create table siste_sykmeldingstatus
(
    oppfolgingsperiode_uuid varchar primary key references oppfolgingsperiode (uuid),
    fnr                     varchar                             not null,
    startdato               date                                not null,
    sluttdato               date                                not null,
    har_arbeidsgiver        boolean                             not null,
    created_at              timestamp default current_timestamp not null,
    updated_at              timestamp default current_timestamp not null
);