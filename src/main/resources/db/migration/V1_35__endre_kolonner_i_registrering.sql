ALTER TABLE BRUKER_REGISTRERING add HAR_HELSEUTFORDRINGER NUMBER(1,0);
ALTER TABLE BRUKER_REGISTRERING add HAR_JOBBET_SAMMENHENGENDE NUMBER(1,0);
ALTER TABLE BRUKER_REGISTRERING add SITUASJON VARCHAR(15);

ALTER TABLE BRUKER_REGISTRERING DROP COLUMN UTDANNINGSNIVA;
ALTER TABLE BRUKER_REGISTRERING DROP COLUMN BESVARELSE;
