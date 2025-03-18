DO
$$
    BEGIN
        IF (SELECT exists(SELECT rolname FROM pg_roles WHERE rolname = 'cloudsqliamuser'))
        THEN
            ALTER DEFAULT PRIVILEGES IN SCHEMA veilarboppfolging GRANT SELECT ON TABLES TO "cloudsqliamuser";
            GRANT SELECT ON ALL TABLES IN SCHEMA veilarboppfolging TO "cloudsqliamuser";
        END IF;
    END
$$;