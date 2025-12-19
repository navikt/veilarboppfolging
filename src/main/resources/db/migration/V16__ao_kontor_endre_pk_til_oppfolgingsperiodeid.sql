ALTER TABLE veilarboppfolging.ao_kontor
    DROP CONSTRAINT ao_kontor_pkey;

ALTER TABLE veilarboppfolging.ao_kontor
    ADD CONSTRAINT ao_kontor_oppfolgingsperiode_pk
        PRIMARY KEY (oppfolgingsperiode_id);