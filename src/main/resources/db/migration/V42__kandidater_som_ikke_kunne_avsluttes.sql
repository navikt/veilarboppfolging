create table kandidater_som_ikke_kunne_avsluttes (
    oppfolgingsperiode_uuid char(36) references oppfolgingsperiode(uuid) not null,
    siste_utmeldingshendelse_id uuid references kandidater_for_utmelding_hendelser(utmeldingshendelse_id) not null,
    created_at timestamp default current_timestamp not null
);