create table aktive_arbeidssokerperioder
(
    arbeidssokerperiode_id varchar primary key,
    aktor_id               varchar                             not null,
    fnr                    varchar                             not null,
    startet_tidspunkt      timestamp                           not null,
    created_at             timestamp default current_timestamp not null,
    updated_at             timestamp default current_timestamp not null
);