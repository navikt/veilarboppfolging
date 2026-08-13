
INSERT INTO kandidater_for_utmelding_hendelser (
    utmeldingshendelse_id,
    hendelse,
    hendelse_data,
    utfort_av,
    utfort_av_type,
    kilde,
    oppfolgingsperiode_uuid,
    created_at
)
SELECT
    gen_random_uuid(),
    kfu.hendelse,
    CASE
        WHEN kfu.detaljer IS NOT NULL THEN jsonb_build_object('detaljer', kfu.detaljer)
        ELSE NULL
    END,
    NULL,
    kfu.avsluttet_av,
    kfu.kilde,
    kfu.oppfolgingsperiode_uuid,
    COALESCE(kfu.created_at::timestamp, current_timestamp)
FROM kandidat_for_utmelding kfu;

INSERT INTO kandidater_for_utmelding (
    forlenget_til,
    oppfolgingsperiode_uuid,
    siste_utmeldingshendelse_id,
    created_at,
    updated_at
)
SELECT
    NULL,
    kfu.oppfolgingsperiode_uuid,
    m.utmeldingshendelse_id,
    COALESCE(kfu.created_at::timestamp, current_timestamp),
    COALESCE(kfu.updated_at::timestamp, COALESCE(kfu.created_at::timestamp, current_timestamp))
FROM kandidat_for_utmelding kfu
JOIN kandidater_for_utmelding_hendelser m ON m.oppfolgingsperiode_uuid = kfu.oppfolgingsperiode_uuid;
