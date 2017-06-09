CREATE TABLE OPPFOLGINGSPERIODE (
  oppfolgingsperiodeId VARCHAR(20) NOT NULL,
  aktoerId VARCHAR(20) NOT NULL,
  sluttDato DATE NOT NULL,
  begrunnelse VARCHAR(2000) NOT NULL,
  PRIMARY KEY (oppfolgingsperiodeId)
);

