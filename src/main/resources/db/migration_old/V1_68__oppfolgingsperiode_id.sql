
-- This is an inefficient way of storing UUID, but it should not be a problem at the current scale
-- After migrating to postgres a proper column type can be used
ALTER TABLE OPPFOLGINGSPERIODE ADD UUID CHAR(36);
