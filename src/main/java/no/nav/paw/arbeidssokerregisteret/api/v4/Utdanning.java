/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package no.nav.paw.arbeidssokerregisteret.api.v4;

import org.apache.avro.generic.GenericArray;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.util.Utf8;
import org.apache.avro.message.BinaryMessageEncoder;
import org.apache.avro.message.BinaryMessageDecoder;
import org.apache.avro.message.SchemaStore;

@org.apache.avro.specific.AvroGenerated
public class Utdanning extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = 611238057966436218L;


  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Utdanning\",\"namespace\":\"no.nav.paw.arbeidssokerregisteret.api.v4\",\"fields\":[{\"name\":\"nus\",\"type\":\"string\",\"doc\":\"NUS kode for utdanning, oversikt over NUS koder er tilgjengelig fra SSB.\"},{\"name\":\"bestaatt\",\"type\":[\"null\",{\"type\":\"enum\",\"name\":\"JaNeiVetIkke\",\"namespace\":\"no.nav.paw.arbeidssokerregisteret.api.v1\",\"doc\":\"Enkel enum som brukes til typisk 'ja', 'nei' eller 'vet ikke' svar.\",\"symbols\":[\"JA\",\"NEI\",\"VET_IKKE\"]}],\"doc\":\"Bare inkludert dersom informasjonen er innhetet, feks for\\nnus kode 0,1,2 og 9 gir det ikke mening å hente inn info for\\ndette feltet.\\n\",\"default\":null},{\"name\":\"godkjent\",\"type\":[\"null\",\"no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke\"],\"doc\":\"Bare inkludert dersom informasjonen er innhetet, feks for\\nnus kode 0,1,2 og 9 gir det ikke mening å hente inn info for\\ndette feltet.\\n\",\"default\":null}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }

  private static final SpecificData MODEL$ = new SpecificData();

  private static final BinaryMessageEncoder<Utdanning> ENCODER =
      new BinaryMessageEncoder<>(MODEL$, SCHEMA$);

  private static final BinaryMessageDecoder<Utdanning> DECODER =
      new BinaryMessageDecoder<>(MODEL$, SCHEMA$);

  /**
   * Return the BinaryMessageEncoder instance used by this class.
   * @return the message encoder used by this class
   */
  public static BinaryMessageEncoder<Utdanning> getEncoder() {
    return ENCODER;
  }

  /**
   * Return the BinaryMessageDecoder instance used by this class.
   * @return the message decoder used by this class
   */
  public static BinaryMessageDecoder<Utdanning> getDecoder() {
    return DECODER;
  }

  /**
   * Create a new BinaryMessageDecoder instance for this class that uses the specified {@link SchemaStore}.
   * @param resolver a {@link SchemaStore} used to find schemas by fingerprint
   * @return a BinaryMessageDecoder instance for this class backed by the given SchemaStore
   */
  public static BinaryMessageDecoder<Utdanning> createDecoder(SchemaStore resolver) {
    return new BinaryMessageDecoder<>(MODEL$, SCHEMA$, resolver);
  }

  /**
   * Serializes this Utdanning to a ByteBuffer.
   * @return a buffer holding the serialized data for this instance
   * @throws java.io.IOException if this instance could not be serialized
   */
  public java.nio.ByteBuffer toByteBuffer() throws java.io.IOException {
    return ENCODER.encode(this);
  }

  /**
   * Deserializes a Utdanning from a ByteBuffer.
   * @param b a byte buffer holding serialized data for an instance of this class
   * @return a Utdanning instance decoded from the given buffer
   * @throws java.io.IOException if the given bytes could not be deserialized into an instance of this class
   */
  public static Utdanning fromByteBuffer(
      java.nio.ByteBuffer b) throws java.io.IOException {
    return DECODER.decode(b);
  }

  /** NUS kode for utdanning, oversikt over NUS koder er tilgjengelig fra SSB. */
  private java.lang.CharSequence nus;
  /** Bare inkludert dersom informasjonen er innhetet, feks for
nus kode 0,1,2 og 9 gir det ikke mening å hente inn info for
dette feltet.
 */
  private no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke bestaatt;
  /** Bare inkludert dersom informasjonen er innhetet, feks for
nus kode 0,1,2 og 9 gir det ikke mening å hente inn info for
dette feltet.
 */
  private no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke godkjent;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public Utdanning() {}

  /**
   * All-args constructor.
   * @param nus NUS kode for utdanning, oversikt over NUS koder er tilgjengelig fra SSB.
   * @param bestaatt Bare inkludert dersom informasjonen er innhetet, feks for
nus kode 0,1,2 og 9 gir det ikke mening å hente inn info for
dette feltet.

   * @param godkjent Bare inkludert dersom informasjonen er innhetet, feks for
nus kode 0,1,2 og 9 gir det ikke mening å hente inn info for
dette feltet.

   */
  public Utdanning(java.lang.CharSequence nus, no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke bestaatt, no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke godkjent) {
    this.nus = nus;
    this.bestaatt = bestaatt;
    this.godkjent = godkjent;
  }

  @Override
  public org.apache.avro.specific.SpecificData getSpecificData() { return MODEL$; }

  @Override
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }

  // Used by DatumWriter.  Applications should not call.
  @Override
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return nus;
    case 1: return bestaatt;
    case 2: return godkjent;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  // Used by DatumReader.  Applications should not call.
  @Override
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: nus = (java.lang.CharSequence)value$; break;
    case 1: bestaatt = (no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke)value$; break;
    case 2: godkjent = (no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke)value$; break;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  /**
   * Gets the value of the 'nus' field.
   * @return NUS kode for utdanning, oversikt over NUS koder er tilgjengelig fra SSB.
   */
  public java.lang.CharSequence getNus() {
    return nus;
  }


  /**
   * Sets the value of the 'nus' field.
   * NUS kode for utdanning, oversikt over NUS koder er tilgjengelig fra SSB.
   * @param value the value to set.
   */
  public void setNus(java.lang.CharSequence value) {
    this.nus = value;
  }

  /**
   * Gets the value of the 'bestaatt' field.
   * @return Bare inkludert dersom informasjonen er innhetet, feks for
nus kode 0,1,2 og 9 gir det ikke mening å hente inn info for
dette feltet.

   */
  public no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke getBestaatt() {
    return bestaatt;
  }


  /**
   * Sets the value of the 'bestaatt' field.
   * Bare inkludert dersom informasjonen er innhetet, feks for
nus kode 0,1,2 og 9 gir det ikke mening å hente inn info for
dette feltet.

   * @param value the value to set.
   */
  public void setBestaatt(no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke value) {
    this.bestaatt = value;
  }

  /**
   * Gets the value of the 'godkjent' field.
   * @return Bare inkludert dersom informasjonen er innhetet, feks for
nus kode 0,1,2 og 9 gir det ikke mening å hente inn info for
dette feltet.

   */
  public no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke getGodkjent() {
    return godkjent;
  }


  /**
   * Sets the value of the 'godkjent' field.
   * Bare inkludert dersom informasjonen er innhetet, feks for
nus kode 0,1,2 og 9 gir det ikke mening å hente inn info for
dette feltet.

   * @param value the value to set.
   */
  public void setGodkjent(no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke value) {
    this.godkjent = value;
  }

  /**
   * Creates a new Utdanning RecordBuilder.
   * @return A new Utdanning RecordBuilder
   */
  public static no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning.Builder newBuilder() {
    return new no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning.Builder();
  }

  /**
   * Creates a new Utdanning RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new Utdanning RecordBuilder
   */
  public static no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning.Builder newBuilder(no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning.Builder other) {
    if (other == null) {
      return new no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning.Builder();
    } else {
      return new no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning.Builder(other);
    }
  }

  /**
   * Creates a new Utdanning RecordBuilder by copying an existing Utdanning instance.
   * @param other The existing instance to copy.
   * @return A new Utdanning RecordBuilder
   */
  public static no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning.Builder newBuilder(no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning other) {
    if (other == null) {
      return new no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning.Builder();
    } else {
      return new no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning.Builder(other);
    }
  }

  /**
   * RecordBuilder for Utdanning instances.
   */
  @org.apache.avro.specific.AvroGenerated
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<Utdanning>
    implements org.apache.avro.data.RecordBuilder<Utdanning> {

    /** NUS kode for utdanning, oversikt over NUS koder er tilgjengelig fra SSB. */
    private java.lang.CharSequence nus;
    /** Bare inkludert dersom informasjonen er innhetet, feks for
nus kode 0,1,2 og 9 gir det ikke mening å hente inn info for
dette feltet.
 */
    private no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke bestaatt;
    /** Bare inkludert dersom informasjonen er innhetet, feks for
nus kode 0,1,2 og 9 gir det ikke mening å hente inn info for
dette feltet.
 */
    private no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke godkjent;

    /** Creates a new Builder */
    private Builder() {
      super(SCHEMA$, MODEL$);
    }

    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.nus)) {
        this.nus = data().deepCopy(fields()[0].schema(), other.nus);
        fieldSetFlags()[0] = other.fieldSetFlags()[0];
      }
      if (isValidValue(fields()[1], other.bestaatt)) {
        this.bestaatt = data().deepCopy(fields()[1].schema(), other.bestaatt);
        fieldSetFlags()[1] = other.fieldSetFlags()[1];
      }
      if (isValidValue(fields()[2], other.godkjent)) {
        this.godkjent = data().deepCopy(fields()[2].schema(), other.godkjent);
        fieldSetFlags()[2] = other.fieldSetFlags()[2];
      }
    }

    /**
     * Creates a Builder by copying an existing Utdanning instance
     * @param other The existing instance to copy.
     */
    private Builder(no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning other) {
      super(SCHEMA$, MODEL$);
      if (isValidValue(fields()[0], other.nus)) {
        this.nus = data().deepCopy(fields()[0].schema(), other.nus);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.bestaatt)) {
        this.bestaatt = data().deepCopy(fields()[1].schema(), other.bestaatt);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.godkjent)) {
        this.godkjent = data().deepCopy(fields()[2].schema(), other.godkjent);
        fieldSetFlags()[2] = true;
      }
    }

    /**
      * Gets the value of the 'nus' field.
      * NUS kode for utdanning, oversikt over NUS koder er tilgjengelig fra SSB.
      * @return The value.
      */
    public java.lang.CharSequence getNus() {
      return nus;
    }


    /**
      * Sets the value of the 'nus' field.
      * NUS kode for utdanning, oversikt over NUS koder er tilgjengelig fra SSB.
      * @param value The value of 'nus'.
      * @return This builder.
      */
    public no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning.Builder setNus(java.lang.CharSequence value) {
      validate(fields()[0], value);
      this.nus = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /**
      * Checks whether the 'nus' field has been set.
      * NUS kode for utdanning, oversikt over NUS koder er tilgjengelig fra SSB.
      * @return True if the 'nus' field has been set, false otherwise.
      */
    public boolean hasNus() {
      return fieldSetFlags()[0];
    }


    /**
      * Clears the value of the 'nus' field.
      * NUS kode for utdanning, oversikt over NUS koder er tilgjengelig fra SSB.
      * @return This builder.
      */
    public no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning.Builder clearNus() {
      nus = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'bestaatt' field.
      * Bare inkludert dersom informasjonen er innhetet, feks for
nus kode 0,1,2 og 9 gir det ikke mening å hente inn info for
dette feltet.

      * @return The value.
      */
    public no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke getBestaatt() {
      return bestaatt;
    }


    /**
      * Sets the value of the 'bestaatt' field.
      * Bare inkludert dersom informasjonen er innhetet, feks for
nus kode 0,1,2 og 9 gir det ikke mening å hente inn info for
dette feltet.

      * @param value The value of 'bestaatt'.
      * @return This builder.
      */
    public no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning.Builder setBestaatt(no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke value) {
      validate(fields()[1], value);
      this.bestaatt = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /**
      * Checks whether the 'bestaatt' field has been set.
      * Bare inkludert dersom informasjonen er innhetet, feks for
nus kode 0,1,2 og 9 gir det ikke mening å hente inn info for
dette feltet.

      * @return True if the 'bestaatt' field has been set, false otherwise.
      */
    public boolean hasBestaatt() {
      return fieldSetFlags()[1];
    }


    /**
      * Clears the value of the 'bestaatt' field.
      * Bare inkludert dersom informasjonen er innhetet, feks for
nus kode 0,1,2 og 9 gir det ikke mening å hente inn info for
dette feltet.

      * @return This builder.
      */
    public no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning.Builder clearBestaatt() {
      bestaatt = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    /**
      * Gets the value of the 'godkjent' field.
      * Bare inkludert dersom informasjonen er innhetet, feks for
nus kode 0,1,2 og 9 gir det ikke mening å hente inn info for
dette feltet.

      * @return The value.
      */
    public no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke getGodkjent() {
      return godkjent;
    }


    /**
      * Sets the value of the 'godkjent' field.
      * Bare inkludert dersom informasjonen er innhetet, feks for
nus kode 0,1,2 og 9 gir det ikke mening å hente inn info for
dette feltet.

      * @param value The value of 'godkjent'.
      * @return This builder.
      */
    public no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning.Builder setGodkjent(no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke value) {
      validate(fields()[2], value);
      this.godkjent = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /**
      * Checks whether the 'godkjent' field has been set.
      * Bare inkludert dersom informasjonen er innhetet, feks for
nus kode 0,1,2 og 9 gir det ikke mening å hente inn info for
dette feltet.

      * @return True if the 'godkjent' field has been set, false otherwise.
      */
    public boolean hasGodkjent() {
      return fieldSetFlags()[2];
    }


    /**
      * Clears the value of the 'godkjent' field.
      * Bare inkludert dersom informasjonen er innhetet, feks for
nus kode 0,1,2 og 9 gir det ikke mening å hente inn info for
dette feltet.

      * @return This builder.
      */
    public no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning.Builder clearGodkjent() {
      godkjent = null;
      fieldSetFlags()[2] = false;
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Utdanning build() {
      try {
        Utdanning record = new Utdanning();
        record.nus = fieldSetFlags()[0] ? this.nus : (java.lang.CharSequence) defaultValue(fields()[0]);
        record.bestaatt = fieldSetFlags()[1] ? this.bestaatt : (no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke) defaultValue(fields()[1]);
        record.godkjent = fieldSetFlags()[2] ? this.godkjent : (no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke) defaultValue(fields()[2]);
        return record;
      } catch (org.apache.avro.AvroMissingFieldException e) {
        throw e;
      } catch (java.lang.Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumWriter<Utdanning>
    WRITER$ = (org.apache.avro.io.DatumWriter<Utdanning>)MODEL$.createDatumWriter(SCHEMA$);

  @Override public void writeExternal(java.io.ObjectOutput out)
    throws java.io.IOException {
    WRITER$.write(this, SpecificData.getEncoder(out));
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumReader<Utdanning>
    READER$ = (org.apache.avro.io.DatumReader<Utdanning>)MODEL$.createDatumReader(SCHEMA$);

  @Override public void readExternal(java.io.ObjectInput in)
    throws java.io.IOException {
    READER$.read(this, SpecificData.getDecoder(in));
  }

  @Override protected boolean hasCustomCoders() { return true; }

  @Override public void customEncode(org.apache.avro.io.Encoder out)
    throws java.io.IOException
  {
    out.writeString(this.nus);

    if (this.bestaatt == null) {
      out.writeIndex(0);
      out.writeNull();
    } else {
      out.writeIndex(1);
      out.writeEnum(this.bestaatt.ordinal());
    }

    if (this.godkjent == null) {
      out.writeIndex(0);
      out.writeNull();
    } else {
      out.writeIndex(1);
      out.writeEnum(this.godkjent.ordinal());
    }

  }

  @Override public void customDecode(org.apache.avro.io.ResolvingDecoder in)
    throws java.io.IOException
  {
    org.apache.avro.Schema.Field[] fieldOrder = in.readFieldOrderIfDiff();
    if (fieldOrder == null) {
      this.nus = in.readString(this.nus instanceof Utf8 ? (Utf8)this.nus : null);

      if (in.readIndex() != 1) {
        in.readNull();
        this.bestaatt = null;
      } else {
        this.bestaatt = no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke.values()[in.readEnum()];
      }

      if (in.readIndex() != 1) {
        in.readNull();
        this.godkjent = null;
      } else {
        this.godkjent = no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke.values()[in.readEnum()];
      }

    } else {
      for (int i = 0; i < 3; i++) {
        switch (fieldOrder[i].pos()) {
        case 0:
          this.nus = in.readString(this.nus instanceof Utf8 ? (Utf8)this.nus : null);
          break;

        case 1:
          if (in.readIndex() != 1) {
            in.readNull();
            this.bestaatt = null;
          } else {
            this.bestaatt = no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke.values()[in.readEnum()];
          }
          break;

        case 2:
          if (in.readIndex() != 1) {
            in.readNull();
            this.godkjent = null;
          } else {
            this.godkjent = no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke.values()[in.readEnum()];
          }
          break;

        default:
          throw new java.io.IOException("Corrupt ResolvingDecoder.");
        }
      }
    }
  }
}










