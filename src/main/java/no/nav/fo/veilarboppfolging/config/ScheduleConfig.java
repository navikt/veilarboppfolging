package no.nav.fo.veilarboppfolging.config;

import lombok.extern.slf4j.Slf4j;
import no.nav.fo.veilarboppfolging.schedule.AvsluttOppfolgingKafkaFeilSchedule;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Slf4j
@Configuration
@Import({ AvsluttOppfolgingKafkaFeilSchedule.class })
public class ScheduleConfig {}
