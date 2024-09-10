CREATE TABLE ESKALERINGSVARSEL (
  varsel_id NUMBER GENERATED ALWAYS AS IDENTITY(START WITH 1 INCREMENT BY 1),
  aktor_id NVARCHAR2(255),
  opprettet_av NVARCHAR2(255),
  opprettet_dato TIMESTAMP,
  avsluttet_dato TIMESTAMP,
  tilhorende_dialog_id NUMBER(19),
  PRIMARY KEY (varsel_id)
);

ALTER TABLE SITUASJON ADD gjeldende_eskaleringsvarsel NUMBER;