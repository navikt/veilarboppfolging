ALTER TABLE SITUASJON ADD GJELDENDE_MAL NUMBER;

CREATE TABLE MAL (
  ID NUMBER NOT NULL,
  AKTORID VARCHAR(20) NOT NULL,
  MAL VARCHAR(500),
  ENDRET_AV VARCHAR(20),
  DATO TIMESTAMP,
  PRIMARY KEY (ID),
  FOREIGN KEY (AKTORID) REFERENCES SITUASJON (AKTORID)
);

CREATE SEQUENCE MAL_SEQ START WITH 1;