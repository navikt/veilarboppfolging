package no.nav.fo.veilarboppfolging.vilkar;

import no.nav.sbl.tekster.Filleser;
import no.nav.sbl.tekster.Utils;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.ResourceBundle;

import static java.util.Optional.ofNullable;
import static no.nav.fo.veilarboppfolging.vilkar.VilkarService.VilkarType.UNDER_OPPFOLGING;

@Component
public class VilkarService {

    public String getVilkar(VilkarType type, String sprak) {
        ResourceBundle resourceBundle = new Filleser("/", "veilarboppfolging-vilkar").lesFiler(ofNullable(sprak).orElse("nb"));
        Properties properties = Utils.convertResourceBundleToProperties(resourceBundle);
        return properties.getProperty(ofNullable(type).orElse(UNDER_OPPFOLGING).filnavn);
    }

    public enum VilkarType {
        UNDER_OPPFOLGING("vilkar"),
        PRIVAT("vilkar-privat");

        public final String filnavn;

        VilkarType(String filnavn) {
            this.filnavn = filnavn;
        }
    }

}
