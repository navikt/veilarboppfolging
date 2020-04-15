package no.nav.fo.veilarboppfolging.services;

import no.nav.tjeneste.virksomhet.varseloppgave.v1.binding.BestillVarselOppgaveUgyldigInput;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.binding.VarseloppgaveV1;
import no.nav.tjeneste.virksomhet.varseloppgave.v1.meldinger.BestillVarselOppgaveRequest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.fo.veilarboppfolging.config.ApplicationConfig.ARBEIDSRETTET_DIALOG_PROPERTY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EskaleringsvarselServiceTest {

    private static final String AKTOR_ID = "1234";
    private static final int DIALOG_ID = 1;

    @BeforeClass
    public static void setUp() throws Exception {
        System.setProperty(ARBEIDSRETTET_DIALOG_PROPERTY, "https://arbeidsrettet_dialog.no");
    }

    @InjectMocks
    private EskaleringsvarselService eskaleringsvarselService;

    @Mock
    private VarseloppgaveV1 varseloppgaveV1;

    @Test
    public void sendEskaleringsvarsel() throws Exception {
        eskaleringsvarselService.sendEskaleringsvarsel(AKTOR_ID, DIALOG_ID);
        verify(varseloppgaveV1, times(DIALOG_ID))
                .bestillVarselOppgave(any(BestillVarselOppgaveRequest.class));
    }

    @Test(expected = BestillVarselOppgaveUgyldigInput.class)
    public void sendEskaleringFeiler() throws Exception {
        when(varseloppgaveV1.bestillVarselOppgave(any())).thenThrow(BestillVarselOppgaveUgyldigInput.class);
        eskaleringsvarselService.sendEskaleringsvarsel(AKTOR_ID, DIALOG_ID);
    }

    @Test
    public void dialogUrl() throws Exception {
        assertThat(
                eskaleringsvarselService.dialogUrl(DIALOG_ID),
                equalTo("https://arbeidsrettet_dialog.no/1")
        );
    }
}
