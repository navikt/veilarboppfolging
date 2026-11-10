create table siste_sykmeldingstatus
(
    aktor_id         varchar primary key,
    fnr              varchar                             not null,
    startdato        date                                not null,
    sluttdato        date                                not null,
    har_arbeidsgiver boolean                             not null,
    created_at       timestamp default current_timestamp not null,
    updated_at       timestamp default current_timestamp not null
);