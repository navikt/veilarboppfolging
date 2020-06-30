package no.nav.veilarboppfolging.controller.domain;


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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Dato dato = (Dato) o;

        if (getYear() != dato.getYear()) return false;
        if (getMonth() != dato.getMonth()) return false;
        return getDay() == dato.getDay();

    }

    @Override
    public int hashCode() {
        int result = getYear();
        result = 31 * result + getMonth();
        result = 31 * result + getDay();
        return result;
    }
}
