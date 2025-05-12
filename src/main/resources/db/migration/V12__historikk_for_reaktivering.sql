create table historikk_for_reaktivering
(
    id                     serial primary key,
    aktor_id               varchar(20)  not null,
    oppfolgingsperiode     char(36)     not null,
    reaktivering_tidspunkt timestamp(6) not null,
    reaktivert_av          varchar(20)  not null
);
