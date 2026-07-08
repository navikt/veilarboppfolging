alter table oppfolgingstatus drop column innsatsgruppe;

create table innsatsgruppe (
    aktor_id varchar(20) not null primary key,
    innsatsgruppe varchar(40) not null,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp,
    FOREIGN KEY (aktor_id) REFERENCES oppfolgingstatus(aktor_id)
)
