ALTER TABLE KVP DROP CONSTRAINT FK_OPPRETTET_KODEVERKBRUKER;
ALTER TABLE KVP DROP CONSTRAINT FK_AVSLUTTET_KODEVERKBRUKER;
ALTER TABLE MANUELL_STATUS DROP CONSTRAINT FK_OPPRETTET_AV;

DROP TABLE KODEVERK_BRUKER
