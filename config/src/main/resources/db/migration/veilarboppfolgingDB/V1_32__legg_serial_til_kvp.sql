-- Create a new sequence for SERIAL fields.
CREATE SEQUENCE KVP_SERIAL_SEQ;

-- Create the SERIAL field on the KVP table, and populate it with initial data.
ALTER TABLE KVP ADD SERIAL NUMBER;
CREATE UNIQUE INDEX KVP_SERIAL_INDEX ON KVP (SERIAL);
UPDATE KVP SET SERIAL = KVP_SERIAL_SEQ.nextval;

-- Alter the SERIAL field so that NULL is not allowed.
ALTER TABLE KVP MODIFY SERIAL NUMBER NOT NULL;


-- To remove changes:
--
--ALTER TABLE KVP DROP COLUMN SERIAL;
--DROP SEQUENCE KVP_SERIAL_SEQ;

