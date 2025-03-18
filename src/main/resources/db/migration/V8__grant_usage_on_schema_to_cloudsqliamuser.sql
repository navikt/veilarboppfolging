DO
$$
    BEGIN
        IF (SELECT exists(SELECT rolname FROM pg_roles WHERE rolname = 'cloudsqliamuser'))
        THEN
            GRANT USAGE ON SCHEMA veilarboppfolging to "cloudsqliamuser";
        END IF;
    END
$$;