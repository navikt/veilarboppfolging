package no.nav.veilarboppfolging.repository.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.time.ZonedDateTime;

@AllArgsConstructor
@Getter
@ToString
public class UtmeldingEntity {
    public final String aktor_Id;
    public final ZonedDateTime iservSiden;
}

