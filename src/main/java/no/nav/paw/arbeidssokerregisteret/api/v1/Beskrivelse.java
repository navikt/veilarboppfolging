/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package no.nav.paw.arbeidssokerregisteret.api.v1;
/** Beskrivelse av jobbsituasjonen. Følgende beskrivelser er definert:
UKJENT_VERDI					-		Verdien er satt, men den er ikke definert i versjonen av APIet som klienten bruker.
UDEFINERT						-		Verdien er ikke satt.
HAR_SAGT_OPP					-		Personen har sagt opp sin stilling.
HAR_BLITT_SAGT_OPP				-		Personen har blitt sagt opp fra sin stilling.
ER_PERMITTERT					-		Personen er permittert.
ALDRI_HATT_JOBB					-		Personen har aldri hatt en jobb.
IKKE_VAERT_I_JOBB_SISTE_2_AAR	-		Personen har ikke vært i jobb de siste 2 årene.
AKKURAT_FULLFORT_UTDANNING		-		Personen har akkurat fullført sin utdanning.
USIKKER_JOBBSITUASJON			-		Personen er usikker på sin jobbsituasjon.
MIDLERTIDIG_JOBB				-		Personen har en midlertidig jobb.
DELTIDSJOBB_VIL_MER				-		Personen har en/flere deltidsjobber, men ønsker å jobbe mer.
NY_JOBB							-		Personen har fått seg ny jobb.
KONKURS							-		Personen har mistet jobben på grunn av konkurs.
ANNET							-		Personen har en annen jobbsituasjon. */
@org.apache.avro.specific.AvroGenerated
public enum Beskrivelse implements org.apache.avro.generic.GenericEnumSymbol<Beskrivelse> {
  UKJENT_VERDI, UDEFINERT, HAR_SAGT_OPP, HAR_BLITT_SAGT_OPP, ER_PERMITTERT, ALDRI_HATT_JOBB, IKKE_VAERT_I_JOBB_SISTE_2_AAR, AKKURAT_FULLFORT_UTDANNING, VIL_BYTTE_JOBB, USIKKER_JOBBSITUASJON, MIDLERTIDIG_JOBB, DELTIDSJOBB_VIL_MER, NY_JOBB, KONKURS, ANNET  ;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"enum\",\"name\":\"Beskrivelse\",\"namespace\":\"no.nav.paw.arbeidssokerregisteret.api.v1\",\"doc\":\"Beskrivelse av jobbsituasjonen. Følgende beskrivelser er definert:\\nUKJENT_VERDI\\t\\t\\t\\t\\t-\\t\\tVerdien er satt, men den er ikke definert i versjonen av APIet som klienten bruker.\\nUDEFINERT\\t\\t\\t\\t\\t\\t-\\t\\tVerdien er ikke satt.\\nHAR_SAGT_OPP\\t\\t\\t\\t\\t-\\t\\tPersonen har sagt opp sin stilling.\\nHAR_BLITT_SAGT_OPP\\t\\t\\t\\t-\\t\\tPersonen har blitt sagt opp fra sin stilling.\\nER_PERMITTERT\\t\\t\\t\\t\\t-\\t\\tPersonen er permittert.\\nALDRI_HATT_JOBB\\t\\t\\t\\t\\t-\\t\\tPersonen har aldri hatt en jobb.\\nIKKE_VAERT_I_JOBB_SISTE_2_AAR\\t-\\t\\tPersonen har ikke vært i jobb de siste 2 årene.\\nAKKURAT_FULLFORT_UTDANNING\\t\\t-\\t\\tPersonen har akkurat fullført sin utdanning.\\nUSIKKER_JOBBSITUASJON\\t\\t\\t-\\t\\tPersonen er usikker på sin jobbsituasjon.\\nMIDLERTIDIG_JOBB\\t\\t\\t\\t-\\t\\tPersonen har en midlertidig jobb.\\nDELTIDSJOBB_VIL_MER\\t\\t\\t\\t-\\t\\tPersonen har en/flere deltidsjobber, men ønsker å jobbe mer.\\nNY_JOBB\\t\\t\\t\\t\\t\\t\\t-\\t\\tPersonen har fått seg ny jobb.\\nKONKURS\\t\\t\\t\\t\\t\\t\\t-\\t\\tPersonen har mistet jobben på grunn av konkurs.\\nANNET\\t\\t\\t\\t\\t\\t\\t-\\t\\tPersonen har en annen jobbsituasjon.\",\"symbols\":[\"UKJENT_VERDI\",\"UDEFINERT\",\"HAR_SAGT_OPP\",\"HAR_BLITT_SAGT_OPP\",\"ER_PERMITTERT\",\"ALDRI_HATT_JOBB\",\"IKKE_VAERT_I_JOBB_SISTE_2_AAR\",\"AKKURAT_FULLFORT_UTDANNING\",\"VIL_BYTTE_JOBB\",\"USIKKER_JOBBSITUASJON\",\"MIDLERTIDIG_JOBB\",\"DELTIDSJOBB_VIL_MER\",\"NY_JOBB\",\"KONKURS\",\"ANNET\"],\"default\":\"UKJENT_VERDI\"}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }

  @Override
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
}
