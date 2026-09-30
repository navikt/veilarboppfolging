update kandidater_som_ikke_kunne_avsluttes
set begrunnelse = 'BRUKER_VAR_IKKE_UNDER_OPPFOLGING'
where begrunnelse = 'bruker var ikke under oppfølging';

update kandidater_som_ikke_kunne_avsluttes
set begrunnelse = 'BRUKER_HAR_AKTIVE_TILTAKSDELTAKELSER'
where begrunnelse = 'bruker hadde aktive tiltaksdeltakelser';

update kandidater_som_ikke_kunne_avsluttes
set begrunnelse = 'BRUKER_ER_DELTAKER_I_UNGDOMSPROGRAMMET'
where begrunnelse = 'bruker er deltaker i ungdomsprogrammet';

update kandidater_som_ikke_kunne_avsluttes
set begrunnelse = 'BRUKER_ER_REGISTRERT_SOM_ARBEIDSSOKER'
where begrunnelse = 'bruker er registrert som arbeidssøker';

update kandidater_som_ikke_kunne_avsluttes
set begrunnelse = 'BRUKER_MOTTAR_ELLER_HAR_SOKT_OM_AAP'
where begrunnelse = 'bruker mottar eller har søkt om AAP';

update kandidater_som_ikke_kunne_avsluttes
set begrunnelse = 'OPPFOLGINGEN_ER_FORLENGET'
where begrunnelse = 'oppfølgingen er forlenget';

update kandidater_som_ikke_kunne_avsluttes
set begrunnelse = 'BRUKER_VAR_UNDER_KVP'
where begrunnelse = 'bruker var under kvp';