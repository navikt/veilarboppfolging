package no.nav.veilarboppfolging.controller.v2;

import no.nav.veilarboppfolging.service.ArenaYtelserService;
import no.nav.veilarboppfolging.service.AuthService;
import no.nav.veilarboppfolging.utils.auth.AuthorizationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest(controllers = YtelseV2Controller.class)
class YtelseV2ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    AuthorizationInterceptor authorizationInterceptor;

    @MockBean
    private ArenaYtelserService arenaYtelserService;

    //TODO: Trenger vi endepunktet?
//    @Test
//    void hentYtelser() throws Exception{
//        Fnr fnr = Fnr.of("1234");
//        YtelserRequest ytelserRequest = new YtelserRequest(fnr);
//        YtelseskontraktResponse response = getKomplettResponse();
//        YtelserV2Response response2 = new YtelserV2Response(response.getVedtaksliste(), response.getYtelser());
//        when(authorizationInterceptor.preHandle(any(),any(),any())).thenReturn(true);
//        when(arenaYtelserService.hentYtelser(fnr)).thenReturn(response);
//        String expJson = JsonUtils.toJson(response2);
//        mockMvc.perform(post("/api/v2/person/hent-ytelser")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(JsonUtils.toJson(ytelserRequest))
//                )
//                .andExpect(status().is(HttpStatus.OK_200))
//                .andExpect(content().json(expJson));
//    }

}