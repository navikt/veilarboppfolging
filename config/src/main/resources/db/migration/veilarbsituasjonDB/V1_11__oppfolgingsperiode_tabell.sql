CREATE TABLE OPPFOLGINGSPERIODE (
  aktorId NVARCHAR2(255) NOT NULL,
  veileder VARCHAR(20) NOT NULL,
  sluttDato DATE NOT NULL,
  begrunnelse NVARCHAR2(500) NOT NULL,
  oppdatert TIMESTAMP(3) NOT NULL
);

