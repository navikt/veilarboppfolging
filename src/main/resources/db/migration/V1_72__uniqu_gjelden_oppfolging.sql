delete from OPPFOLGINGSPERIODE a1 where exists(select * from  OPPFOLGINGSPERIODE a2 where a1.AKTOR_ID = a2.AKTOR_ID and  a1.SLUTTDATO = a2.SLUTTDATO  and  a1.STARTDATO < a2.STARTDATO);
delete from OPPFOLGINGSPERIODE a1 where exists(select * from  OPPFOLGINGSPERIODE a2 where a1.AKTOR_ID = a2.AKTOR_ID and  a1.SLUTTDATO is null  and a2.SLUTTDATO is null  and  a1.STARTDATO < a2.STARTDATO);

alter table OPPFOLGINGSPERIODE ADD CONSTRAINT uniqeGjeldendeOppfolgingsPeriode UNIQUE (aktor_id, sluttdato);