package no.nav.fo.veilarboppfolging.vilkar;

import no.nav.sbl.dialogarena.types.Pingable;
import no.nav.sbl.dialogarena.types.Pingable.Ping.PingMetadata;
import no.nav.sbl.tekster.Filleser;
import no.nav.sbl.tekster.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.ResourceBundle;

import static java.util.Optional.ofNullable;
import static no.nav.fo.veilarboppfolging.vilkar.VilkarService.VilkarType.UNDER_OPPFOLGING;

@Component
public class VilkarService implements Pingable {

    @Value("${vilkar.path}")
    String vilkarPath;


    public String getVilkar(VilkarType type, String sprak) {
        ResourceBundle resourceBundle = new Filleser(vilkarPath + "/tekster", "veilarboppfolging-vilkar").lesFiler(ofNullable(sprak).orElse("nb"));
        Properties properties = Utils.convertResourceBundleToProperties(resourceBundle);
        return properties.getProperty(ofNullable(type).orElse(UNDER_OPPFOLGING).filnavn);
    }

    @Override
    public Ping ping() {
        PingMetadata metadata = new PingMetadata(
                "Vilkår: " + vilkarPath,
                "Sjekker om filen (tekstressursen) for vilkår finnes på noden",
                false
        );
        try {
            String vilkar = getVilkar(null,null);
            if (vilkar == null || vilkar.trim().length() == 0) {
                throw new IllegalStateException("mangler vilkår");
            }
            return Ping.lyktes(metadata);
        } catch (Exception e) {
            return Ping.feilet(metadata, e);
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
