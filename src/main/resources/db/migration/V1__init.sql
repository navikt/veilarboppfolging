create sequence oppfolgingsenhet_endret_rowid_seq
    increment by -1;

alter sequence oppfolgingsenhet_endret_rowid_seq owner to veilarboppfolging;

create sequence veileder_tilordninger_rowid_seq
    increment by -1;

alter sequence veileder_tilordninger_rowid_seq owner to veilarboppfolging;

create sequence bruker_med_flere_aktorid_seq
    start with 101
    cache 1;

alter sequence bruker_med_flere_aktorid_seq owner to veilarboppfolging;

create sequence bruker_oppslag_med_flere_aktorid_seq
    cache 1;

alter sequence bruker_oppslag_med_flere_aktorid_seq owner to veilarboppfolging;

create sequence bruker_registrering_seq
    cache 1;

alter sequence bruker_registrering_seq owner to veilarboppfolging;

create sequence brukervilkar_seq
    start with 201
    cache 1;

alter sequence brukervilkar_seq owner to veilarboppfolging;

create sequence enhet_seq
    start with 857316
    cache 1;

alter sequence enhet_seq owner to veilarboppfolging;

create sequence eskaleringsvarsel_seq
    start with 708
    cache 1;

alter sequence eskaleringsvarsel_seq owner to veilarboppfolging;

create sequence iseq$$_134636
    start with 2085
    cache 1;

alter sequence iseq$$_134636 owner to veilarboppfolging;

create sequence iseq$$_93913
    cache 1;

alter sequence iseq$$_93913 owner to veilarboppfolging;

create sequence kafka_consumer_record_id_seq
    start with 1410541
    cache 1;

alter sequence kafka_consumer_record_id_seq owner to veilarboppfolging;

create sequence kafka_producer_record_id_seq
    start with 3602364
    cache 1;

alter sequence kafka_producer_record_id_seq owner to veilarboppfolging;

create sequence kvp_seq
    start with 1697
    cache 1;

alter sequence kvp_seq owner to veilarboppfolging;

create sequence kvp_serial_seq
    start with 2049
    cache 1;

alter sequence kvp_serial_seq owner to veilarboppfolging;

create sequence mal_seq
    start with 96940
    cache 1;

alter sequence mal_seq owner to veilarboppfolging;

create sequence status_seq
    start with 119479
    cache 1;

alter sequence status_seq owner to veilarboppfolging;

create sequence veileder_tilordning_seq
    start with 6812
    cache 1;

alter sequence veileder_tilordning_seq owner to veilarboppfolging;

create table bruker_med_flere_aktorid
(
    bruker_seq        numeric     not null
        constraint sys_c0014394
            primary key,
    oppslag_bruker_id varchar(13) not null
        constraint bruker_uq
            unique,
    created           timestamp(6)
);

alter table bruker_med_flere_aktorid
    owner to veilarboppfolging;

create table eskaleringsvarsel
(
    varsel_id             numeric not null
        constraint sys_c0010277
            primary key,
    aktor_id              varchar(255),
    opprettet_av          varchar(255),
    opprettet_dato        timestamp(6),
    avsluttet_dato        timestamp(6),
    tilhorende_dialog_id  bigint,
    opprettet_begrunnelse text,
    avsluttet_begrunnelse text,
    avsluttet_av          varchar(255)
);

alter table eskaleringsvarsel
    owner to veilarboppfolging;

create index esk_varsel_aktoerid_index
    on eskaleringsvarsel ((aktor_id::character varying));

create table kafka_consumer_record
(
    id               bigint                                                                not null
        constraint sys_c0015696
            primary key,
    topic            varchar(100)                                                          not null,
    partition        integer                                                               not null,
    record_offset    bigint                                                                not null,
    retries          integer      default 0                                                not null,
    last_retry       timestamp(6),
    key              bytea,
    value            bytea,
    headers_json     text,
    record_timestamp bigint,
    created_at       timestamp(6) default (CURRENT_TIMESTAMP)::timestamp without time zone not null,
    constraint sys_c0015697
        unique (topic, partition, record_offset)
);

alter table kafka_consumer_record
    owner to veilarboppfolging;

create index kafka_consumer_record_topic_partition_idx
    on kafka_consumer_record (topic, partition);

create table kafka_producer_record
(
    id           bigint                                                                not null
        constraint sys_c0015701
            primary key,
    topic        varchar(100)                                                          not null,
    key          bytea,
    value        bytea,
    headers_json text,
    created_at   timestamp(6) default (CURRENT_TIMESTAMP)::timestamp without time zone not null
);

alter table kafka_producer_record
    owner to veilarboppfolging;

create table kodeverk_bruker
(
    bruker_kode        varchar(255) not null
        constraint kodeverk_bruker_bruker_kode_pk
            primary key,
    bruker_beskrivelse varchar(255) not null,
    opprettet          timestamp(6) not null,
    opprettet_av       varchar(255) not null,
    endret             timestamp(6) not null,
    endret_av          varchar(255) not null
);

alter table kodeverk_bruker
    owner to veilarboppfolging;

create table kvp
(
    kvp_id                   numeric      not null
        constraint kvp_pk
            primary key,
    aktor_id                 varchar(20)  not null,
    enhet                    varchar(255) not null,
    opprettet_av             varchar(255),
    opprettet_dato           timestamp(6),
    opprettet_begrunnelse    text,
    avsluttet_av             varchar(255),
    avsluttet_dato           timestamp(6),
    avsluttet_begrunnelse    text,
    serial                   numeric      not null,
    opprettet_kodeverkbruker varchar(255)
        constraint fk_opprettet_kodeverkbruker
            references kodeverk_bruker,
    avsluttet_kodeverkbruker varchar(255)
        constraint fk_avsluttet_kodeverkbruker
            references kodeverk_bruker
);

alter table kvp
    owner to veilarboppfolging;

create index kvp_aktoerid_index
    on kvp (aktor_id);

create unique index kvp_serial_index
    on kvp (serial);

create table oppfolgingsenhet_endret
(
    aktor_id    varchar(20),
    enhet       varchar(20)                                                                                  not null,
    endret_dato timestamp(6),
    enhet_seq   numeric,
    rowid       numeric(33) default nextval('oppfolgingsenhet_endret_rowid_seq'::regclass) not null
        constraint veilarboppfolging_oppfolgingsenhet_endret_pk_rowid
            primary key
);

alter table oppfolgingsenhet_endret
    owner to veilarboppfolging;

alter sequence oppfolgingsenhet_endret_rowid_seq owned by oppfolgingsenhet_endret.rowid;

create table oppfolgingstatus
(
    aktor_id                    varchar(20)        not null
        constraint sys_c0010240
            primary key,
    under_oppfolging            smallint           not null,
    gjeldende_manuell_status    numeric,
    gjeldende_mal               numeric,
    veileder                    varchar(20),
    oppdatert                   timestamp(3),
    gjeldende_eskaleringsvarsel numeric
        constraint eskaleringsvarsel_fk
            references eskaleringsvarsel,
    gjeldende_kvp               numeric
        constraint kvp_fk
            references kvp,
    ny_for_veileder             smallint default 0 not null,
    sist_tilordnet              timestamp(6)
);

alter table oppfolgingstatus
    owner to veilarboppfolging;

alter table kvp
    add constraint kvp_aktor_id_fk
        foreign key (aktor_id) references oppfolgingstatus;

create table mal
(
    id        numeric      not null
        constraint sys_c0010251
            primary key,
    aktor_id  varchar(20)  not null
        constraint sys_c0010252
            references oppfolgingstatus,
    mal       varchar(500),
    endret_av varchar(20)  not null,
    dato      timestamp(6) not null
);

alter table mal
    owner to veilarboppfolging;

create index mal_aktoerid_index
    on mal (aktor_id);

create table manuell_status
(
    id                    numeric      not null
        constraint sys_c0010247
            primary key,
    aktor_id              varchar(20)  not null
        constraint sys_c0010248
            references oppfolgingstatus,
    manuell               smallint,
    opprettet_dato        timestamp(6),
    begrunnelse           varchar(500),
    opprettet_av          varchar(255) not null
        constraint fk_opprettet_av
            references kodeverk_bruker,
    opprettet_av_brukerid varchar(255)
);

alter table manuell_status
    owner to veilarboppfolging;

create table oppfolgingsperiode
(
    aktor_id            varchar(20)  not null
        constraint fk_oppfolgingsperiode
            references oppfolgingstatus,
    avslutt_veileder    varchar(20),
    sluttdato           timestamp(6),
    avslutt_begrunnelse varchar(500),
    oppdatert           timestamp(6) not null,
    startdato           timestamp(6),
    uuid                char(36)     not null
        constraint oppfolgingsperiode_uuid_pk
            primary key,
    start_begrunnelse   varchar(30)
);

alter table oppfolgingsperiode
    owner to veilarboppfolging;

create index oppfolgingsperiode_index1
    on oppfolgingsperiode (aktor_id);

create table shedlock
(
    name       varchar(64) not null
        constraint sys_c0010312
            primary key,
    lock_until timestamp(3),
    locked_at  timestamp(3),
    locked_by  varchar(255)
);

alter table shedlock
    owner to veilarboppfolging;

create table siste_endring_oppfoelging_bruker
(
    fodselsnr        varchar(33)  not null
        constraint sys_c0022346
            primary key,
    sist_endret_dato timestamp(6) not null
);

alter table siste_endring_oppfoelging_bruker
    owner to veilarboppfolging;

create table utmelding
(
    aktor_id       varchar(20)  not null
        constraint unique_aktor_id
            unique,
    iserv_fra_dato timestamp(6) not null,
    oppdatert_dato timestamp(6)
);

alter table utmelding
    owner to veilarboppfolging;

create table veileder_tilordninger
(
    aktor_id              varchar(20)                                                                                not null,
    veileder              varchar(20)                                                                                not null,
    sist_tilordnet        timestamp(6),
    tilordning_seq        numeric,
    tilordnet_av_veileder varchar(20),
    rowid                 numeric(33) default nextval('veileder_tilordninger_rowid_seq'::regclass) not null
        constraint veilarboppfolging_veileder_tilordninger_pk_rowid
            primary key
);

alter table veileder_tilordninger
    owner to veilarboppfolging;

alter sequence veileder_tilordninger_rowid_seq owned by veileder_tilordninger.rowid;

create table sak
(
    id                      numeric default nextval('"iseq$$_134636"'::regclass) not null
        constraint sys_c0027842
            primary key,
    oppfolgingsperiode_uuid char(36)
        constraint oppfolgingsperiode_uuid_fk
            references oppfolgingsperiode,
    created_at              timestamp(6)                                                           not null
);

alter table sak
    owner to veilarboppfolging;

INSERT INTO KODEVERK_BRUKER(bruker_kode, bruker_beskrivelse, endret, endret_av, opprettet, opprettet_av)
VALUES('NAV',
       '(veileder i Nav) Benyttes når veileder setter bruker til digital/manuell.',
       CURRENT_TIMESTAMP,
       'KASSERT',
       CURRENT_TIMESTAMP,
       'KASSERT');

INSERT INTO KODEVERK_BRUKER(bruker_kode, bruker_beskrivelse, endret, endret_av, opprettet, opprettet_av)
VALUES('SYSTEM',
       '(applikasjonen selv) Benyttes når bruker automatisk settes til manuell, f.eks dersom sjekk mot KRR gir "reservert".',
       CURRENT_TIMESTAMP,
       'KASSERT',
       CURRENT_TIMESTAMP,
       'KASSERT');

INSERT INTO KODEVERK_BRUKER(bruker_kode, bruker_beskrivelse, endret, endret_av, opprettet, opprettet_av)
VALUES('EKSTERN',
       '(ekstern bruker) Benyttes når bruker selv har satt seg digital.',
       CURRENT_TIMESTAMP,
       'KASSERT',
       CURRENT_TIMESTAMP,
       'KASSERT');
