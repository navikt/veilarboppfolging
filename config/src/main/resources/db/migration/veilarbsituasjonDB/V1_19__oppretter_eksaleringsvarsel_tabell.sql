CREATE TABLE ESKALERINGSVARSEL (
  varsel_id NUMBER,
  aktorid NVARCHAR2(255),
  opprettet_av NVARCHAR2(255),
  opprettet_dato TIMESTAMP,
  avsluttet_dato TIMESTAMP,
  tilhorende_dialog_id NUMBER(19),
  gjeldende NUMBER(1,0),
  PRIMARY KEY (varsel_id)
)