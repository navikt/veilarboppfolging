CREATE SEQUENCE KVP_SERIAL_SEQ START WITH 1;
ALTER TABLE KVP ADD SERIAL NUMBER;
UPDATE KVP SET SERIAL = (SELECT KVP_SEQ.nextval FROM DUAL);
ALTER TABLE KVP MODIFY SERIAL NUMBER NOT NULL;

CREATE INDEX KVP_SERIAL_INDEX ON KVP (SERIAL);

-- ALTER TABLE KVP DROP SERIAL;
-- DROP SEQUENCE KVP_SERIAL_SEQ;
