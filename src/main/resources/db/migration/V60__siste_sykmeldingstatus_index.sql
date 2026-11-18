create index if not exists siste_sykmeldingstatus_sistsjekket_sluttdato_arbgiver_idx
    on siste_sykmeldingstatus (sist_sjekket_avslutningsstatus, sluttdato, har_arbeidsgiver);