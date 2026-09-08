UPDATE kandidater_for_utmelding kfu
SET avsluttes_automatisk_dato = kfuh.hendelse_tidspunkt + interval '28 days'
FROM kandidater_for_utmelding_hendelser kfuh
WHERE kfu.siste_utmeldingshendelse_id = kfuh.utmeldingshendelse_id;
