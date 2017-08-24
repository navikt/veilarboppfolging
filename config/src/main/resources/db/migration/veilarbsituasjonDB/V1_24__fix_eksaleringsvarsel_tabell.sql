ALTER TABLE SITUASJON
  DROP COLUMN gjeldende_eskaleringsvarsel;

DROP TABLE ESKALERINGSVARSEL;

CREATE SEQUENCE ESKALERINGSVARSEL_SEQ START WITH 1;

CREATE TABLE ESKALERINGSVARSEL (
  varsel_id            NUMBER NOT NULL,
  aktor_id             NVARCHAR2(255),
  opprettet_av         NVARCHAR2(255),
  opprettet_dato       TIMESTAMP,
  avsluttet_dato       TIMESTAMP,
  tilhorende_dialog_id NUMBER(19),
  PRIMARY KEY (varsel_id)
);

ALTER TABLE SITUASJON
  ADD gjeldende_eskaleringsvarsel NUMBER;

ALTER TABLE SITUASJON
  ADD CONSTRAINT ESKALERINGSVARSEL_FK FOREIGN KEY (gjeldende_eskaleringsvarsel)
REFERENCES ESKALERINGSVARSEL (varsel_id);
