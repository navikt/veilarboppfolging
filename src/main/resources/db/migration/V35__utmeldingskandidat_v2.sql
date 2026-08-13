create table kandidater_for_utmelding_hendelser (
    utmeldingshendelse_id uuid primary key,
    hendelse varchar not null, -- Feks ARBEIDSSOKERPERIODE_AVSLUTTET_IKKE_LEVERT_MELDEKORT
    hendelse_data jsonb, -- Feks { "forlengetTil": "11-11-2020", ... }, kommer an på hendelse typen
    utfort_av varchar, -- ident til veileder eller bruker, mangler i tidlige data
    utfort_av_type varchar not null, -- VEILEDER, BRUKAR
    kilde varchar not null, -- Feks arbeidssøkerregisteret
    oppfolgingsperiode_uuid uuid references oppfolgingsperiode(uuid) not null,
    created_at timestamp default current_timestamp not null
);

create index hendelse_oppfolgingsperiode_idx ON kandidater_for_utmelding_hendelser(oppfolgingsperiode_uuid);

create table kandidater_for_utmelding (
    forlenget_til timestamp,
    oppfolgingsperiode_uuid uuid references oppfolgingsperiode(uuid) not null primary key,
    siste_utmeldingshendelse_id uuid references kandidater_for_utmelding_hendelser(utmeldingshendelse_id) not null,
    created_at timestamp default current_timestamp not null,
    updated_at timestamp default current_timestamp not null
);

create index kandidat_oppfolgingsperiode_idx ON kandidater_for_utmelding(oppfolgingsperiode_uuid);

create table filterkategori_id_mapping (
    kategori varchar not null,
    oppfolgingsperiode_id uuid references oppfolgingsperiode(uuid) not null,
    filterkategori_person_id uuid not null,
    PRIMARY KEY (oppfolgingsperiode_id, kategori)
);

