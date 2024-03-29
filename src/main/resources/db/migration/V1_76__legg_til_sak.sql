CREATE TABLE SAK (
    ID NUMBER GENERATED ALWAYS AS IDENTITY(START WITH 1000 INCREMENT BY 1),
    OPPFOLGINGSPERIODE_UUID CHAR(36),
    CREATED_AT TIMESTAMP NOT NULL,
    CONSTRAINT OPPFOLGINGSPERIODE_UUID_FK FOREIGN KEY (OPPFOLGINGSPERIODE_UUID) REFERENCES OPPFOLGINGSPERIODE (UUID),
    PRIMARY KEY (ID)
);