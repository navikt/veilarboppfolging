create table temp_aktiver_microfrontend (
    aktor_id varchar(20),
    erAktivert boolean,
    startdato_oppfolging timestamp(6)
);

create table temp_deaktiver_microfrontend (
    aktor_id varchar(20),
    erDeaktivert boolean,
    sluttdato_oppfolging timestamp(6)
);