package no.nav.veilarboppfolging.test;

import lombok.SneakyThrows;
import no.nav.sbl.jdbc.Transactor;

public class TestTransactor extends Transactor{

    public TestTransactor() {
        super(null);
    }

    @Override
    @SneakyThrows
    public void inTransaction(Transactor.InTransaction inTransaction) {
        inTransaction.run();
    }

}
