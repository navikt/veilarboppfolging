-- skal bruke primary key som replica identity
ALTER TABLE kandidater_som_ikke_kunne_avsluttes REPLICA IDENTITY DEFAULT;