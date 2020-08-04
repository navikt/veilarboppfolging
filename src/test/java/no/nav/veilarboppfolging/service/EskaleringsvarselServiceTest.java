package no.nav.veilarboppfolging.service;

import no.nav.tjeneste.virksomhet.varseloppgave.v1.binding.BestillVarselOppgaveUgyldigInput;
import no.nav.veilarboppfolging.client.varseloppgave.VarseloppgaveClient;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EskaleringsvarselServiceTest {

    private static final String AKTOR_ID = "1234";
    private static final int DIALOG_ID = 1;

    @BeforeClass
    public static void setUp() {
//        System.setProperty(ARBEIDSRETTET_DIALOG_PROPERTY, "https://arbeidsrettet_dialog.no");
    }

    @Mock
    private VarseloppgaveClient varseloppgaveClient;

    @Test
    public void sendEskaleringsvarsel() {
//        eskaleringsvarselService.sendEskaleringsvarsel(AKTOR_ID, DIALOG_ID);
//        verify(varseloppgaveV1, times(DIALOG_ID))
//                .bestillVarselOppgave(any(BestillVarselOppgaveRequest.class));
    }

    @Test(expected = BestillVarselOppgaveUgyldigInput.class)
    public void sendEskaleringFeiler() {
//        when(varseloppgaveV1.bestillVarselOppgave(any())).thenThrow(BestillVarselOppgaveUgyldigInput.class);
//        eskaleringsvarselService.sendEskaleringsvarsel(AKTOR_ID, DIALOG_ID);
    }

    @Test
    public void dialogUrl() {
//        assertThat(
//                eskaleringsvarselService.dialogUrl(DIALOG_ID),
//                equalTo("https://arbeidsrettet_dialog.no/1")
//        );
    }
}
