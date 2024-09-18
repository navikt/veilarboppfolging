CREATE SEQUENCE BRUKER_MED_FLERE_AKTORID_SEQ START WITH 1;

CREATE TABLE BRUKER_MED_FLERE_AKTORID(
    BRUKER_SEQ NUMBER,
    OPPSLAG_BRUKER_ID VARCHAR(13) not NULL,
    CREATED TIMESTAMP,
    PRIMARY KEY (BRUKER_SEQ),
    CONSTRAINT BRUKER_UQ UNIQUE (OPPSLAG_BRUKER_ID)
);

UPDATE BRUKER_MED_FLERE_AKTORID SET BRUKER_SEQ = BRUKER_MED_FLERE_AKTORID_SEQ.NEXTVAL;