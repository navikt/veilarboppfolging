/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package no.nav.paw.arbeidssokerregisteret.api.v1;

import org.apache.avro.generic.GenericArray;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.util.Utf8;
import org.apache.avro.message.BinaryMessageEncoder;
import org.apache.avro.message.BinaryMessageDecoder;
import org.apache.avro.message.SchemaStore;

/** En periode er en tidsperiode hvor en bruker har vært registrert som arbeidssøker.
En bruker kan ha flere perioder, og en periode kan være pågående eller avsluttet.
En periode er pågående dersom "avsluttet" er 'null' (ikke satt). */
@org.apache.avro.specific.AvroGenerated
public class Periode extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = -743291843548587429L;


  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Periode\",\"namespace\":\"no.nav.paw.arbeidssokerregisteret.api.v1\",\"doc\":\"En periode er en tidsperiode hvor en bruker har vært registrert som arbeidssøker.\\nEn bruker kan ha flere perioder, og en periode kan være pågående eller avsluttet.\\nEn periode er pågående dersom \\\"avsluttet\\\" er 'null' (ikke satt).\",\"fields\":[{\"name\":\"id\",\"type\":{\"type\":\"string\",\"logicalType\":\"uuid\"},\"doc\":\"Unik identifikator for perioden.\\nAnnen data knyttet til perioden kan lagres i andre systemer med denne som nøkkel.\\nOpplysninger som hentes inn fra arbeidssøkeren vil være knyttet til denne perioden,\\ndet samme gjelder profileringsresultater.\"},{\"name\":\"identitetsnummer\",\"type\":\"string\",\"doc\":\"Inneholder informasjon om hvem perioden tilhører (fødselsnummer eller d-nummer)\"},{\"name\":\"startet\",\"type\":{\"type\":\"record\",\"name\":\"Metadata\",\"doc\":\"Inneholder metadata om en endring i arbeidssøkerregisteret.\",\"fields\":[{\"name\":\"tidspunkt\",\"type\":{\"type\":\"long\",\"logicalType\":\"timestamp-millis\"},\"doc\":\"Tidspunkt for endringen.\"},{\"name\":\"utfoertAv\",\"type\":{\"type\":\"record\",\"name\":\"Bruker\",\"doc\":\"En bruker er en person eller et system. Personer kan være sluttbrukere eller veiledere.\",\"fields\":[{\"name\":\"type\",\"type\":{\"type\":\"enum\",\"name\":\"BrukerType\",\"symbols\":[\"UKJENT_VERDI\",\"UDEFINERT\",\"VEILEDER\",\"SYSTEM\",\"SLUTTBRUKER\"],\"default\":\"UKJENT_VERDI\"},\"doc\":\"Angir hvilken type bruker det er snakk om\"},{\"name\":\"id\",\"type\":\"string\",\"doc\":\"Brukerens identifikator.\\nFor sluttbruker er dette typisk fødselsnummer eller D-nummer.\\nFor system vil det rett og slett være navnet på et system, eventuelt med versjonsnummer i tillegg (APP_NAVN:VERSJON).\\nFor veileder vil det være NAV identen til veilederen.\"}]}},{\"name\":\"kilde\",\"type\":\"string\",\"doc\":\"Navn på systemet som utførte endringen eller ble benyttet til å utføre endringen.\"},{\"name\":\"aarsak\",\"type\":\"string\",\"doc\":\"Aarasek til endringen. Feks \\\"Flyttet ut av landet\\\" eller lignende.\"},{\"name\":\"tidspunktFraKilde\",\"type\":[\"null\",{\"type\":\"record\",\"name\":\"TidspunktFraKilde\",\"fields\":[{\"name\":\"tidspunkt\",\"type\":{\"type\":\"long\",\"logicalType\":\"timestamp-millis\"},\"doc\":\"Tidspunktet melding ideelt sett skulle vært registert på.\"},{\"name\":\"avviksType\",\"type\":{\"type\":\"enum\",\"name\":\"AvviksType\",\"doc\":\"Ukjent verdi settes aldri direkte, men brukes som standardverdi og\\nfor å indikere at en verdi er ukjent for mottaker av melding, dvs at\\nat den er satt til en verdi som ikke er definert i Avro-skjemaet til mottaker.\\n\\nFORSINKELSE - Grunnen til avvik mellom kilde og register er generell forsinkelse\\n\\t\\t\\t\\t som oppstår i asynkrone systemer.\\n\\nRETTING - \\tGrunnen til avvik mellom kilde og register er at en feil i kilde er rettet\\n             med virking bakover i tid.\",\"symbols\":[\"UKJENT_VERDI\",\"FORSINKELSE\",\"RETTING\"],\"default\":\"UKJENT_VERDI\"},\"doc\":\"Årsaken til til avvik i tid mellom kilde og register.\"}]}],\"doc\":\"Avvik i tid mellom kilde og register.\",\"default\":null}]},\"doc\":\"Inneholder informasjon om når perioden startet og hvem som startet den\"},{\"name\":\"avsluttet\",\"type\":[\"null\",\"Metadata\"],\"doc\":\"Dersom det er en pågående periode, vil denne være 'null'. Er den ikke 'null', er perioden avsluttet.\\nInneholder informasjon om når perioden ble avsluttet og hvem som avsluttet den\"}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }

  private static final SpecificData MODEL$ = new SpecificData();
  static {
    MODEL$.addLogicalTypeConversion(new org.apache.avro.Conversions.UUIDConversion());
    MODEL$.addLogicalTypeConversion(new org.apache.avro.data.TimeConversions.TimestampMillisConversion());
  }

  private static final BinaryMessageEncoder<Periode> ENCODER =
      new BinaryMessageEncoder<>(MODEL$, SCHEMA$);

  private static final BinaryMessageDecoder<Periode> DECODER =
      new BinaryMessageDecoder<>(MODEL$, SCHEMA$);

  /**
   * Return the BinaryMessageEncoder instance used by this class.
   * @return the message encoder used by this class
   */
  public static BinaryMessageEncoder<Periode> getEncoder() {
    return ENCODER;
  }

  /**
   * Return the BinaryMessageDecoder instance used by this class.
   * @return the message decoder used by this class
   */
  public static BinaryMessageDecoder<Periode> getDecoder() {
    return DECODER;
  }

  /**
   * Create a new BinaryMessageDecoder instance for this class that uses the specified {@link SchemaStore}.
   * @param resolver a {@link SchemaStore} used to find schemas by fingerprint
   * @return a BinaryMessageDecoder instance for this class backed by the given SchemaStore
   */
  public static BinaryMessageDecoder<Periode> createDecoder(SchemaStore resolver) {
    return new BinaryMessageDecoder<>(MODEL$, SCHEMA$, resolver);
  }

  /**
   * Serializes this Periode to a ByteBuffer.
   * @return a buffer holding the serialized data for this instance
   * @throws java.io.IOException if this instance could not be serialized
   */
  public java.nio.ByteBuffer toByteBuffer() throws java.io.IOException {
    return ENCODER.encode(this);
  }

  /**
   * Deserializes a Periode from a ByteBuffer.
   * @param b a byte buffer holding serialized data for an instance of this class
   * @return a Periode instance decoded from the given buffer
   * @throws java.io.IOException if the given bytes could not be deserialized into an instance of this class
   */
  public static Periode fromByteBuffer(
      java.nio.ByteBuffer b) throws java.io.IOException {
    return DECODER.decode(b);
  }

  /** Unik identifikator for perioden.
Annen data knyttet til perioden kan lagres i andre systemer med denne som nøkkel.
Opplysninger som hentes inn fra arbeidssøkeren vil være knyttet til denne perioden,
det samme gjelder profileringsresultater. */
  private java.util.UUID id;
  /** Inneholder informasjon om hvem perioden tilhører (fødselsnummer eller d-nummer) */
  private java.lang.CharSequence identitetsnummer;
  /** Inneholder informasjon om når perioden startet og hvem som startet den */
  private no.nav.paw.arbeidssokerregisteret.api.v1.Metadata startet;
  /** Dersom det er en pågående periode, vil denne være 'null'. Er den ikke 'null', er perioden avsluttet.
Inneholder informasjon om når perioden ble avsluttet og hvem som avsluttet den */
  private no.nav.paw.arbeidssokerregisteret.api.v1.Metadata avsluttet;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public Periode() {}

  /**
   * All-args constructor.
   * @param id Unik identifikator for perioden.
Annen data knyttet til perioden kan lagres i andre systemer med denne som nøkkel.
Opplysninger som hentes inn fra arbeidssøkeren vil være knyttet til denne perioden,
det samme gjelder profileringsresultater.
   * @param identitetsnummer Inneholder informasjon om hvem perioden tilhører (fødselsnummer eller d-nummer)
   * @param startet Inneholder informasjon om når perioden startet og hvem som startet den
   * @param avsluttet Dersom det er en pågående periode, vil denne være 'null'. Er den ikke 'null', er perioden avsluttet.
Inneholder informasjon om når perioden ble avsluttet og hvem som avsluttet den
   */
  public Periode(java.util.UUID id, java.lang.CharSequence identitetsnummer, no.nav.paw.arbeidssokerregisteret.api.v1.Metadata startet, no.nav.paw.arbeidssokerregisteret.api.v1.Metadata avsluttet) {
    this.id = id;
    this.identitetsnummer = identitetsnummer;
    this.startet = startet;
    this.avsluttet = avsluttet;
  }

  @Override
  public org.apache.avro.specific.SpecificData getSpecificData() { return MODEL$; }

  @Override
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }

  // Used by DatumWriter.  Applications should not call.
  @Override
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return id;
    case 1: return identitetsnummer;
    case 2: return startet;
    case 3: return avsluttet;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  private static final org.apache.avro.Conversion<?>[] conversions =
      new org.apache.avro.Conversion<?>[] {
      new org.apache.avro.Conversions.UUIDConversion(),
      null,
      null,
      null,
      null
  };

  @Override
  public org.apache.avro.Conversion<?> getConversion(int field) {
    return conversions[field];
  }

  // Used by DatumReader.  Applications should not call.
  @Override
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: id = (java.util.UUID)value$; break;
    case 1: identitetsnummer = (java.lang.CharSequence)value$; break;
    case 2: startet = (no.nav.paw.arbeidssokerregisteret.api.v1.Metadata)value$; break;
    case 3: avsluttet = (no.nav.paw.arbeidssokerregisteret.api.v1.Metadata)value$; break;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  /**
   * Gets the value of the 'id' field.
   * @return Unik identifikator for perioden.
Annen data knyttet til perioden kan lagres i andre systemer med denne som nøkkel.
Opplysninger som hentes inn fra arbeidssøkeren vil være knyttet til denne perioden,
det samme gjelder profileringsresultater.
   */
  public java.util.UUID getId() {
    return id;
  }


  /**
   * Sets the value of the 'id' field.
   * Unik identifikator for perioden.
Annen data knyttet til perioden kan lagres i andre systemer med denne som nøkkel.
Opplysninger som hentes inn fra arbeidssøkeren vil være knyttet til denne perioden,
det samme gjelder profileringsresultater.
   * @param value the value to set.
   */
  public void setId(java.util.UUID value) {
    this.id = value;
  }

  /**
   * Gets the value of the 'identitetsnummer' field.
   * @return Inneholder informasjon om hvem perioden tilhører (fødselsnummer eller d-nummer)
   */
  public java.lang.CharSequence getIdentitetsnummer() {
    return identitetsnummer;
  }


  /**
   * Sets the value of the 'identitetsnummer' field.
   * Inneholder informasjon om hvem perioden tilhører (fødselsnummer eller d-nummer)
   * @param value the value to set.
   */
  public void setIdentitetsnummer(java.lang.CharSequence value) {
    this.identitetsnummer = value;
  }

  /**
   * Gets the value of the 'startet' field.
   * @return Inneholder informasjon om når perioden startet og hvem som startet den
   */
  public no.nav.paw.arbeidssokerregisteret.api.v1.Metadata getStartet() {
    return startet;
  }


  /**
   * Sets the value of the 'startet' field.
   * Inneholder informasjon om når perioden startet og hvem som startet den
   * @param value the value to set.
   */
  public void setStartet(no.nav.paw.arbeidssokerregisteret.api.v1.Metadata value) {
    this.startet = value;
  }

  /**
   * Gets the value of the 'avsluttet' field.
   * @return Dersom det er en pågående periode, vil denne være 'null'. Er den ikke 'null', er perioden avsluttet.
Inneholder informasjon om når perioden ble avsluttet og hvem som avsluttet den
   */
  public no.nav.paw.arbeidssokerregisteret.api.v1.Metadata getAvsluttet() {
    return avsluttet;
  }


  /**
   * Sets the value of the 'avsluttet' field.
   * Dersom det er en pågående periode, vil denne være 'null'. Er den ikke 'null', er perioden avsluttet.
Inneholder informasjon om når perioden ble avsluttet og hvem som avsluttet den
   * @param value the value to set.
   */
  public void setAvsluttet(no.nav.paw.arbeidssokerregisteret.api.v1.Metadata value) {
    this.avsluttet = value;
  }

  /**
   * Creates a new Periode RecordBuilder.
   * @return A new Periode RecordBuilder
   */
  public static no.nav.paw.arbeidssokerregisteret.api.v1.Periode.Builder newBuilder() {
    return new no.nav.paw.arbeidssokerregisteret.api.v1.Periode.Builder();
  }

  /**
   * Creates a new Periode RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new Periode RecordBuilder
   */
  public static no.nav.paw.arbeidssokerregisteret.api.v1.Periode.Builder newBuilder(no.nav.paw.arbeidssokerregisteret.api.v1.Periode.Builder other) {
    if (other == null) {
      return new no.nav.paw.arbeidssokerregisteret.api.v1.Periode.Builder();
    } else {
      return new no.nav.paw.arbeidssokerregisteret.api.v1.Periode.Builder(other);
    }
  }

  /**
   * Creates a new Periode RecordBuilder by copying an existing Periode instance.
   * @param other The existing instance to copy.
   * @return A new Periode RecordBuilder
   */
  public static no.nav.paw.arbeidssokerregisteret.api.v1.Periode.Builder newBuilder(no.nav.paw.arbeidssokerregisteret.api.v1.Periode other) {
    if (other == null) {
      return new no.nav.paw.arbeidssokerregisteret.api.v1.Periode.Builder();
    } else {
      return new no.nav.paw.arbeidssokerregisteret.api.v1.Periode.Builder(other);
    }
  }

  /**
   * RecordBuilder for Periode instances.
   */
  @org.apache.avro.specific.AvroGenerated
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<Periode>
    implements org.apache.avro.data.RecordBuilder<Periode> {

    /** Unik identifikator for perioden.
Annen data knyttet til perioden kan lagres i andre systemer med denne som nøkkel.
Opplysninger som hentes inn fra arbeidssøkeren vil være knyttet til denne perioden,
det samme gjelder profileringsresultater. */
    private java.util.UUID id;
    /** Inneholder informasjon om hvem perioden tilhører (fødselsnummer eller d-nummer) */
    private java.lang.CharSequence identitetsnummer;
    /** Inneholder informasjon om når perioden startet og hvem som startet den */
    private no.nav.paw.arbeidssokerregisteret.api.v1.Metadata startet;
    private no.nav.paw.arbeidssokerregisteret.api.v1.Metadata.Builder startetBuilder;
    /** Dersom det er en pågående periode, vil denne være 'null'. Er den ikke 'null', er perioden avsluttet.
Inneholder informasjon om når perioden ble avsluttet og hvem som avsluttet den */
    private no.nav.paw.arbeidssokerregisteret.api.v1.Metadata avsluttet;
    private no.nav.paw.arbeidssokerregisteret.api.v1.Metadata.Builder avsluttetBuilder;

    /** Creates a new Builder */
    private Builder() {
      super(SCHEMA$, MODEL$);
    }

    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(no.nav.paw.arbeidssokerregisteret.api.v1.Periode.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.id)) {
        this.id = data().deepCopy(fields()[0].schema(), other.id);
        fieldSetFlags()[0] = other.fieldSetFlags()[0];
      }
      if (isValidValue(fields()[1], other.identitetsnummer)) {
        this.identitetsnummer = data().deepCopy(fields()[1].schema(), other.identitetsnummer);
        fieldSetFlags()[1] = other.fieldSetFlags()[1];
      }
      if (isValidValue(fields()[2], other.startet)) {
        this.startet = data().deepCopy(fields()[2].schema(), other.startet);
        fieldSetFlags()[2] = other.fieldSetFlags()[2];
      }
      if (other.hasStartetBuilder()) {
        this.startetBuilder = no.nav.paw.arbeidssokerregisteret.api.v1.Metadata.newBuilder(other.getStartetBuilder());
      }
      if (isValidValue(fields()[3], other.avsluttet)) {
        this.avsluttet = data().deepCopy(fields()[3].schema(), other.avsluttet);
        fieldSetFlags()[3] = other.fieldSetFlags()[3];
      }
      if (other.hasAvsluttetBuilder()) {
        this.avsluttetBuilder = no.nav.paw.arbeidssokerregisteret.api.v1.Metadata.newBuilder(other.getAvsluttetBuilder());
      }
    }

    /**
     * Creates a Builder by copying an existing Periode instance
     * @param other The existing instance to copy.
     */
    private Builder(no.nav.paw.arbeidssokerregisteret.api.v1.Periode other) {
      super(SCHEMA$, MODEL$);
      if (isValidValue(fields()[0], other.id)) {
        this.id = data().deepCopy(fields()[0].schema(), other.id);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.identitetsnummer)) {
        this.identitetsnummer = data().deepCopy(fields()[1].schema(), other.identitetsnummer);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.startet)) {
        this.startet = data().deepCopy(fields()[2].schema(), other.startet);
        fieldSetFlags()[2] = true;
      }
      this.startetBuilder = null;
      if (isValidValue(fields()[3], other.avsluttet)) {
        this.avsluttet = data().deepCopy(fields()[3].schema(), other.avsluttet);
        fieldSetFlags()[3] = true;
      }
      this.avsluttetBuilder = null;
    }

    /**
      * Gets the value of the 'id' field.
      * Unik identifikator for perioden.
Annen data knyttet til perioden kan lagres i andre systemer med denne som nøkkel.
Opplysninger som hentes inn fra arbeidssøkeren vil være knyttet til denne perioden,
det samme gjelder profileringsresultater.
      * @return The value.
      */
    public java.util.UUID getId() {
      return id;
    }


    /**
      * Sets the value of the 'id' field.
      * Unik identifikator for perioden.
Annen data knyttet til perioden kan lagres i andre systemer med denne som nøkkel.
Opplysninger som hentes inn fra arbeidssøkeren vil være knyttet til denne perioden,
det samme gjelder profileringsresultater.
      * @param value The value of 'id'.
      * @return This builder.
      */
    public no.nav.paw.arbeidssokerregisteret.api.v1.Periode.Builder setId(java.util.UUID value) {
      validate(fields()[0], value);
      this.id = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /**
      * Checks whether the 'id' field has been set.
      * Unik identifikator for perioden.
Annen data knyttet til perioden kan lagres i andre systemer med denne som nøkkel.
Opplysninger som hentes inn fra arbeidssøkeren vil være knyttet til denne perioden,
det samme gjelder profileringsresultater.
      * @return True if the 'id' field has been set, false otherwise.
      */
    public boolean hasId() {
      return fieldSetFlags()[0];
    }


    /**
      * Clears the value of the 'id' field.
      * Unik identifikator for perioden.
Annen data knyttet til perioden kan lagres i andre systemer med denne som nøkkel.
Opplysninger som hentes inn fra arbeidssøkeren vil være knyttet til denne perioden,
det samme gjelder profileringsresultater.
      * @return This builder.
      */
    public no.nav.paw.arbeidssokerregisteret.api.v1.Periode.Builder clearId() {
      id = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'identitetsnummer' field.
      * Inneholder informasjon om hvem perioden tilhører (fødselsnummer eller d-nummer)
      * @return The value.
      */
    public java.lang.CharSequence getIdentitetsnummer() {
      return identitetsnummer;
    }


    /**
      * Sets the value of the 'identitetsnummer' field.
      * Inneholder informasjon om hvem perioden tilhører (fødselsnummer eller d-nummer)
      * @param value The value of 'identitetsnummer'.
      * @return This builder.
      */
    public no.nav.paw.arbeidssokerregisteret.api.v1.Periode.Builder setIdentitetsnummer(java.lang.CharSequence value) {
      validate(fields()[1], value);
      this.identitetsnummer = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /**
      * Checks whether the 'identitetsnummer' field has been set.
      * Inneholder informasjon om hvem perioden tilhører (fødselsnummer eller d-nummer)
      * @return True if the 'identitetsnummer' field has been set, false otherwise.
      */
    public boolean hasIdentitetsnummer() {
      return fieldSetFlags()[1];
    }


    /**
      * Clears the value of the 'identitetsnummer' field.
      * Inneholder informasjon om hvem perioden tilhører (fødselsnummer eller d-nummer)
      * @return This builder.
      */
    public no.nav.paw.arbeidssokerregisteret.api.v1.Periode.Builder clearIdentitetsnummer() {
      identitetsnummer = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /**
      * Gets the value of the 'startet' field.
      * Inneholder informasjon om når perioden startet og hvem som startet den
      * @return The value.
      */
    public no.nav.paw.arbeidssokerregisteret.api.v1.Metadata getStartet() {
      return startet;
    }


    /**
      * Sets the value of the 'startet' field.
      * Inneholder informasjon om når perioden startet og hvem som startet den
      * @param value The value of 'startet'.
      * @return This builder.
      */
    public no.nav.paw.arbeidssokerregisteret.api.v1.Periode.Builder setStartet(no.nav.paw.arbeidssokerregisteret.api.v1.Metadata value) {
      validate(fields()[2], value);
      this.startetBuilder = null;
      this.startet = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /**
      * Checks whether the 'startet' field has been set.
      * Inneholder informasjon om når perioden startet og hvem som startet den
      * @return True if the 'startet' field has been set, false otherwise.
      */
    public boolean hasStartet() {
      return fieldSetFlags()[2];
    }

    /**
     * Gets the Builder instance for the 'startet' field and creates one if it doesn't exist yet.
     * Inneholder informasjon om når perioden startet og hvem som startet den
     * @return This builder.
     */
    public no.nav.paw.arbeidssokerregisteret.api.v1.Metadata.Builder getStartetBuilder() {
      if (startetBuilder == null) {
        if (hasStartet()) {
          setStartetBuilder(no.nav.paw.arbeidssokerregisteret.api.v1.Metadata.newBuilder(startet));
        } else {
          setStartetBuilder(no.nav.paw.arbeidssokerregisteret.api.v1.Metadata.newBuilder());
        }
      }
      return startetBuilder;
    }

    /**
     * Sets the Builder instance for the 'startet' field
     * Inneholder informasjon om når perioden startet og hvem som startet den
     * @param value The builder instance that must be set.
     * @return This builder.
     */

    public no.nav.paw.arbeidssokerregisteret.api.v1.Periode.Builder setStartetBuilder(no.nav.paw.arbeidssokerregisteret.api.v1.Metadata.Builder value) {
      clearStartet();
      startetBuilder = value;
      return this;
    }

    /**
     * Checks whether the 'startet' field has an active Builder instance
     * Inneholder informasjon om når perioden startet og hvem som startet den
     * @return True if the 'startet' field has an active Builder instance
     */
    public boolean hasStartetBuilder() {
      return startetBuilder != null;
    }

    /**
      * Clears the value of the 'startet' field.
      * Inneholder informasjon om når perioden startet og hvem som startet den
      * @return This builder.
      */
    public no.nav.paw.arbeidssokerregisteret.api.v1.Periode.Builder clearStartet() {
      startet = null;
      startetBuilder = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    /**
      * Gets the value of the 'avsluttet' field.
      * Dersom det er en pågående periode, vil denne være 'null'. Er den ikke 'null', er perioden avsluttet.
Inneholder informasjon om når perioden ble avsluttet og hvem som avsluttet den
      * @return The value.
      */
    public no.nav.paw.arbeidssokerregisteret.api.v1.Metadata getAvsluttet() {
      return avsluttet;
    }


    /**
      * Sets the value of the 'avsluttet' field.
      * Dersom det er en pågående periode, vil denne være 'null'. Er den ikke 'null', er perioden avsluttet.
Inneholder informasjon om når perioden ble avsluttet og hvem som avsluttet den
      * @param value The value of 'avsluttet'.
      * @return This builder.
      */
    public no.nav.paw.arbeidssokerregisteret.api.v1.Periode.Builder setAvsluttet(no.nav.paw.arbeidssokerregisteret.api.v1.Metadata value) {
      validate(fields()[3], value);
      this.avsluttetBuilder = null;
      this.avsluttet = value;
      fieldSetFlags()[3] = true;
      return this;
    }

    /**
      * Checks whether the 'avsluttet' field has been set.
      * Dersom det er en pågående periode, vil denne være 'null'. Er den ikke 'null', er perioden avsluttet.
Inneholder informasjon om når perioden ble avsluttet og hvem som avsluttet den
      * @return True if the 'avsluttet' field has been set, false otherwise.
      */
    public boolean hasAvsluttet() {
      return fieldSetFlags()[3];
    }

    /**
     * Gets the Builder instance for the 'avsluttet' field and creates one if it doesn't exist yet.
     * Dersom det er en pågående periode, vil denne være 'null'. Er den ikke 'null', er perioden avsluttet.
Inneholder informasjon om når perioden ble avsluttet og hvem som avsluttet den
     * @return This builder.
     */
    public no.nav.paw.arbeidssokerregisteret.api.v1.Metadata.Builder getAvsluttetBuilder() {
      if (avsluttetBuilder == null) {
        if (hasAvsluttet()) {
          setAvsluttetBuilder(no.nav.paw.arbeidssokerregisteret.api.v1.Metadata.newBuilder(avsluttet));
        } else {
          setAvsluttetBuilder(no.nav.paw.arbeidssokerregisteret.api.v1.Metadata.newBuilder());
        }
      }
      return avsluttetBuilder;
    }

    /**
     * Sets the Builder instance for the 'avsluttet' field
     * Dersom det er en pågående periode, vil denne være 'null'. Er den ikke 'null', er perioden avsluttet.
Inneholder informasjon om når perioden ble avsluttet og hvem som avsluttet den
     * @param value The builder instance that must be set.
     * @return This builder.
     */

    public no.nav.paw.arbeidssokerregisteret.api.v1.Periode.Builder setAvsluttetBuilder(no.nav.paw.arbeidssokerregisteret.api.v1.Metadata.Builder value) {
      clearAvsluttet();
      avsluttetBuilder = value;
      return this;
    }

    /**
     * Checks whether the 'avsluttet' field has an active Builder instance
     * Dersom det er en pågående periode, vil denne være 'null'. Er den ikke 'null', er perioden avsluttet.
Inneholder informasjon om når perioden ble avsluttet og hvem som avsluttet den
     * @return True if the 'avsluttet' field has an active Builder instance
     */
    public boolean hasAvsluttetBuilder() {
      return avsluttetBuilder != null;
    }

    /**
      * Clears the value of the 'avsluttet' field.
      * Dersom det er en pågående periode, vil denne være 'null'. Er den ikke 'null', er perioden avsluttet.
Inneholder informasjon om når perioden ble avsluttet og hvem som avsluttet den
      * @return This builder.
      */
    public no.nav.paw.arbeidssokerregisteret.api.v1.Periode.Builder clearAvsluttet() {
      avsluttet = null;
      avsluttetBuilder = null;
      fieldSetFlags()[3] = false;
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Periode build() {
      try {
        Periode record = new Periode();
        record.id = fieldSetFlags()[0] ? this.id : (java.util.UUID) defaultValue(fields()[0]);
        record.identitetsnummer = fieldSetFlags()[1] ? this.identitetsnummer : (java.lang.CharSequence) defaultValue(fields()[1]);
        if (startetBuilder != null) {
          try {
            record.startet = this.startetBuilder.build();
          } catch (org.apache.avro.AvroMissingFieldException e) {
            e.addParentField(record.getSchema().getField("startet"));
            throw e;
          }
        } else {
          record.startet = fieldSetFlags()[2] ? this.startet : (no.nav.paw.arbeidssokerregisteret.api.v1.Metadata) defaultValue(fields()[2]);
        }
        if (avsluttetBuilder != null) {
          try {
            record.avsluttet = this.avsluttetBuilder.build();
          } catch (org.apache.avro.AvroMissingFieldException e) {
            e.addParentField(record.getSchema().getField("avsluttet"));
            throw e;
          }
        } else {
          record.avsluttet = fieldSetFlags()[3] ? this.avsluttet : (no.nav.paw.arbeidssokerregisteret.api.v1.Metadata) defaultValue(fields()[3]);
        }
        return record;
      } catch (org.apache.avro.AvroMissingFieldException e) {
        throw e;
      } catch (java.lang.Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumWriter<Periode>
    WRITER$ = (org.apache.avro.io.DatumWriter<Periode>)MODEL$.createDatumWriter(SCHEMA$);

  @Override public void writeExternal(java.io.ObjectOutput out)
    throws java.io.IOException {
    WRITER$.write(this, SpecificData.getEncoder(out));
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumReader<Periode>
    READER$ = (org.apache.avro.io.DatumReader<Periode>)MODEL$.createDatumReader(SCHEMA$);

  @Override public void readExternal(java.io.ObjectInput in)
    throws java.io.IOException {
    READER$.read(this, SpecificData.getDecoder(in));
  }

}










