-- apply changes
create table totemguard_alerts (
  id                            integer not null,
  check_name                    integer not null,
  totemguard_player_uuid        varchar(40) not null,
  when_created                  timestamp not null,
  constraint ck_totemguard_alerts_check_name check ( check_name in (0,1,2,3,4,5,6,7)),
  constraint pk_totemguard_alerts primary key (id),
  foreign key (totemguard_player_uuid) references totemguard_player (uuid) on delete restrict on update restrict
);

create table totemguard_player (
  uuid                          varchar(40) not null,
  constraint pk_totemguard_player primary key (uuid)
);

create table totemguard_punishments (
  id                            integer not null,
  check_name                    integer not null,
  totemguard_player_uuid        varchar(40) not null,
  when_created                  timestamp not null,
  constraint ck_totemguard_punishments_check_name check ( check_name in (0,1,2,3,4,5,6,7)),
  constraint pk_totemguard_punishments primary key (id),
  foreign key (totemguard_player_uuid) references totemguard_player (uuid) on delete restrict on update restrict
);

