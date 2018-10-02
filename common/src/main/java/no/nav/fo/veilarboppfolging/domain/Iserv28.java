package no.nav.fo.veilarboppfolging.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import java.time.ZonedDateTime;

@AllArgsConstructor
@Getter
@ToString
public class Iserv28 {

    public final String aktor_Id;
    public final ZonedDateTime iservSiden;
}

