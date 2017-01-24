package no.nav.fo.veilarbsituasjon.rest.domain;


@SuppressWarnings("unused")
class Dato {

    private final int year;
    private final int month;
    private final int day;

    Dato(int year, int month, int day) {

        this.year = year;
        this.month = month;
        this.day = day;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }
}
