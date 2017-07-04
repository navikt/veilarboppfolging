INSERT INTO KODEVERK_BRUKER(bruker_kode, bruker_beskrivelse, opprettet, opprettet_av)
VALUES('NAV',
       '(veileder i Nav) Benyttes når veileder setter bruker til digital/manuell.',
       CURRENT_TIMESTAMP,
       '***REMOVED***');

INSERT INTO KODEVERK_BRUKER(bruker_kode, bruker_beskrivelse, opprettet, opprettet_av)
VALUES('SYSTEM',
       '(applikasjonen selv) Benyttes når bruker automatisk settes til manuell, f.eks dersom sjekk mot KRR gir "reservert".',
       CURRENT_TIMESTAMP,
       '***REMOVED***');

INSERT INTO KODEVERK_BRUKER(bruker_kode, bruker_beskrivelse, opprettet, opprettet_av)
VALUES('EKSTERN',
       '(ekstern bruker) Benyttes når bruker selv har satt seg digital.',
       CURRENT_TIMESTAMP,
       '***REMOVED***');
