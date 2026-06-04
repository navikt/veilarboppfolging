CREATE TABLE kandidat_for_utmelding (
    aktor_id varchar primary key,
    hendelse varchar not null,
    created_at timestamp with time zone default current_timestamp,
    updated_at timestamp with time zone default current_timestamp
);