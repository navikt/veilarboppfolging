CREATE SEQUENCE OPPFOLGING_FEED_SEQ ORDER;

ALTER TABLE OPPFOLGINGSTATUS 
ADD COLUMN FEED_ID NUMBER;

UPDATE OPPFOLGINGSTATUS SET FEED_ID = OPPFOLGING_FEED_SEQ.NEXTVAL;