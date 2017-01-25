package no.nav.fo.veilarbsituasjon.rest.domain;


@SuppressWarnings("unused")
public class Vedtak {
    private String vedtakstype;
    private String status;
    private String aktivitetsfase;
    private String rettighetsgruppe;

    public String getVedtakstype() {
        return vedtakstype;
    }

    public String getStatus() {
        return status;
    }

    public String getAktivitetsfase() {
        return aktivitetsfase;
    }

    public String getRettighetsgruppe() {
        return rettighetsgruppe;
    }

    public Vedtak withVedtakstype(String vedtakstype) {
        this.vedtakstype = vedtakstype;
        return this;
    }

    public Vedtak withStatus(String status) {
        this.status = status;
        return this;
    }

    public Vedtak withAktivitetsfase(String aktivitetsfase) {
        this.aktivitetsfase = aktivitetsfase;
        return this;
    }

    public void setRettighetsgruppe(String rettighetsGruppe) {
        this.rettighetsgruppe = rettighetsGruppe;
    }
}
