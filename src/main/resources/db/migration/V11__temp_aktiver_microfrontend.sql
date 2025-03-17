create table temp_aktiver_microfrontend (
    aktor_id varchar(20),
    status varchar(20),
    startdato_oppfolging timestamp(6),
    melding text
);

create table temp_deaktiver_microfrontend (
    aktor_id varchar(20),
    status varchar(20),
    sluttdato_oppfolging timestamp(6),
    melding text
);