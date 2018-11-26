-- Kommentar som endres hvert bygg slik at dette kjøres hver deploy siden checksummen endres:
-- ${project.version}
-- -------------------------


declare
 type versjoner is table of "schema_version"."version"%type;
 produksjonsatte_versjoner versjoner;

 type tn is table of all_tables.table_name%type;
 alle_tabell_navn tn;
begin

 select table_name bulk collect into alle_tabell_navn from all_tables;

 -- last alle produksjonsatte versjoner
 select "version" bulk collect into produksjonsatte_versjoner from "schema_version"
  where
    "success" = 1 and
    ("installed_on" + 1) < current_timestamp  -- det har gått minst en dag siden installasjon
  order by "installed_on" desc;




 if '1.10' member of produksjonsatte_versjoner and 'AKTOER_ID_TO_VEILEDER' member of alle_tabell_navn then
    execute immediate 'DROP TABLE AKTOER_ID_TO_VEILEDER';
 end if;

end;



