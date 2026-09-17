alter table kandidater_for_utmelding add column sist_sjekket timestamp default current_timestamp;

update kandidater_for_utmelding set sist_sjekket = created_at;