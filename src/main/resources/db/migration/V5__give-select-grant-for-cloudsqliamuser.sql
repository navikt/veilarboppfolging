DO
$$
    BEGIN
        IF (SELECT exists(SELECT rolname FROM pg_roles WHERE rolname = 'cloudsqliamuser'))
        THEN
            GRANT SELECT ON ALL TABLES IN SCHEMA veilarboppfolging TO cloudsqliamuser;
        END IF;
    END
$$;