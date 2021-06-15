package no.nav.veilarboppfolging.controller;

import lombok.RequiredArgsConstructor;
import no.nav.common.health.selftest.SelfTestChecks;
import no.nav.common.health.selftest.SelfTestUtils;
import no.nav.common.health.selftest.SelftTestCheckResult;
import no.nav.common.health.selftest.SelftestHtmlGenerator;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static no.nav.common.health.selftest.SelfTestUtils.checkAll;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal")
public class InternalController {

    private final SelfTestChecks selftestChecks;

    @GetMapping("/selftest")
    public ResponseEntity<String> selftest() {
        List<SelftTestCheckResult> checkResults = checkAll(selftestChecks.getSelfTestChecks());
        String html = SelftestHtmlGenerator.generate(checkResults);
        int status = SelfTestUtils.findHttpStatusCode(checkResults, true);

        return ResponseEntity
                .status(status)
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

}
