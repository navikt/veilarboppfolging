package no.nav.fo.veilarbsituasjon.vilkar;

import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.tekster.Filleser;
import no.nav.sbl.tekster.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.ResourceBundle;

import static java.util.Optional.ofNullable;
import static no.nav.fo.veilarbsituasjon.vilkar.VilkarService.VilkarType.UNDER_OPPFOLGING;

@Component
public class VilkarService implements Pingable {

    @Value("${vilkar.path}")
    String vilkarPath;


    public String getVilkar(VilkarType type, String sprak) {
        ResourceBundle resourceBundle = new Filleser(vilkarPath + "/tekster", "veilarbsituasjon-vilkar").lesFiler(ofNullable(sprak).orElse("nb"));
        Properties properties = Utils.convertResourceBundleToProperties(resourceBundle);
        return properties.getProperty(ofNullable(type).orElse(UNDER_OPPFOLGING).filnavn);
    }

    @Override
    public Ping ping() {
        try {
            String vilkar = getVilkar(null,null);
            if (vilkar == null || vilkar.trim().length() == 0) {
                throw new IllegalStateException("mangler vilkår");
            }
            return Ping.lyktes(VilkarService.class.getSimpleName());
        } catch (Exception e) {
            return Ping.feilet(VilkarService.class.getSimpleName(), e);
        }
    }

    public enum VilkarType{
        UNDER_OPPFOLGING("vilkar"),
        PRIVAT("vilkar-privat");

        public final String filnavn;

        VilkarType(String filnavn) {
            this.filnavn = filnavn;
        }
    }

}
