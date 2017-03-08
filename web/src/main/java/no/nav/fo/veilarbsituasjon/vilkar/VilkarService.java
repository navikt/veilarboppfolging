package no.nav.fo.veilarbsituasjon.vilkar;

import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.tekster.Filleser;
import no.nav.sbl.tekster.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.ResourceBundle;

import static java.util.Optional.ofNullable;

@Component
public class VilkarService implements Pingable {

    @Value("${vilkar.path}")
    String vilkarPath;


    public String getVilkar(String sprak) {
        ResourceBundle resourceBundle = new Filleser(vilkarPath + "/tekster", "veilarbsituasjon-vilkar").lesFiler(ofNullable(sprak).orElse("nb"));
        Properties properties = Utils.convertResourceBundleToProperties(resourceBundle);
        return properties.getProperty("vilkar");
    }

    @Override
    public Ping ping() {
        try {
            String vilkar = getVilkar(null);
            if (vilkar == null || vilkar.trim().length() == 0) {
                throw new IllegalStateException("mangler vilkår");
            }
            return Ping.lyktes(VilkarService.class.getSimpleName());
        } catch (Exception e) {
            return Ping.feilet(VilkarService.class.getSimpleName(), e);
        }
    }

}
