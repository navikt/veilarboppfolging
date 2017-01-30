package no.nav.fo.veilarbsituasjon.services;

import no.nav.tjeneste.virksomhet.oppfoelging.v1.OppfoelgingPortType;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;

import static no.nav.fo.veilarbsituasjon.utils.CalendarConverter.convertDateToXMLGregorianCalendar;

public class OppfoelgingServiceTest {


    private static final int MANEDER_BAK_I_TID = 2;
    private static final int MANEDER_FREM_I_TID = 1;

    @InjectMocks
    private OppfoelgingService oppfoelgingService;

    @Mock
    OppfoelgingPortType oppfoelgingPortType;

    @Test
    public void hentOppfoelgingskontraktListe() throws Exception {

        LocalDate periodeFom = LocalDate.now().minusMonths(MANEDER_BAK_I_TID);
        LocalDate periodeTom = LocalDate.now().plusMonths(MANEDER_FREM_I_TID);
        XMLGregorianCalendar fom = convertDateToXMLGregorianCalendar(periodeFom);
        XMLGregorianCalendar tom = convertDateToXMLGregorianCalendar(periodeTom);

//        when(oppfoelgingPortType.hentOppfoelgingskontraktListe(any(WSHentOppfoelgingskontraktListeRequest.class)))
//                .thenReturn(new WSHentOppfoelgingskontraktListeResponse().withOppfoelgingskontraktListe());

        oppfoelgingService.hentOppfoelgingskontraktListe(fom, tom, "***REMOVED***");
    }

}