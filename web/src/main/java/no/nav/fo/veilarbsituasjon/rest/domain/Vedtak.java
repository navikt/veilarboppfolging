package no.nav.fo.veilarbsituasjon.rest.domain;


class Vedtak {
    private String vedtakstype;
    private String status;
    private String aktivitetsfase;
    private String rettighetsGruppe;

    public String getVedtakstype() {
        return vedtakstype;
    }

    public String getStatus() {
        return status;
    }

    public String getAktivitetsfase() {
        return aktivitetsfase;
    }

    public String getRettighetsGruppe() {
        return rettighetsGruppe;
    }

    Vedtak withVedtakstype(String vedtakstype) {
        this.vedtakstype = vedtakstype;
        return this;
    }

    Vedtak withStatus(String status) {
        this.status = status;
        return this;
    }

    Vedtak withAktivitetsfase(String aktivitetsfase) {
        this.aktivitetsfase = aktivitetsfase;
        return this;
    }

    void setRettighetsgruppe(String rettighetsGruppe) {
        this.rettighetsGruppe = rettighetsGruppe;
    }
}
