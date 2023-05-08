package no.nav.veilarboppfolging.client.digdir_krr;

import lombok.Data;
import lombok.experimental.Accessors;
import java.util.List;

@Data
@Accessors(chain = true)
public class PersonIdenter {
	 List<String> personidenter;
}
