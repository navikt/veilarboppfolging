CREATE TABLE SISTE_START_SAMTALE_INNSENDING (
  AKTOR_ID NVARCHAR2(20) NOT NULL,
  DATO     TIMESTAMP(6)  NOT NULL,
  CONSTRAINT "MOTESTOTTE_ID_PK" PRIMARY KEY ("AKTOR_ID")
);