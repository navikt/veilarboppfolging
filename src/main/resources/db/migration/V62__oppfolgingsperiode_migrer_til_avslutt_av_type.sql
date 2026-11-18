UPDATE oppfolgingsperiode
SET avsluttet_av_type = 'SYSTEM'
WHERE avsluttet_av = 'System' AND avsluttet_av_type IS NULL;