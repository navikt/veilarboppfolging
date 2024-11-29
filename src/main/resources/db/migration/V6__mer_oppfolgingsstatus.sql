ALTER TABLE oppfolgingstatus
 ADD COLUMN hovedmaal VARCHAR(8),
 ADD COLUMN innsatsgruppe VARCHAR(5), -- IKVAL, BATT, BFORM, VARIG
 ADD COLUMN servicegruppe VARCHAR(5), -- VURDU, VURDI, OPPFI, IVURD, BKART
 ADD COLUMN kvalifiseringsgruppe VARCHAR(5), -- innsatsgruppe | servicegruppe
 ADD COLUMN formidlingsgruppe VARCHAR(5), -- ARBS, IARBS, ISERV
 ADD COLUMN oppfolgingsenhet VARCHAR(4);
