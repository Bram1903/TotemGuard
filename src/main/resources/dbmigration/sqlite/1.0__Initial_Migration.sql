-- apply changes
create table totemguard_alerts (
  id                            integer not null,
  check_name                    varchar(255) not null,
  totemguard_player_uuid        varchar(40) not null,
  when_created                  timestamp not null,
  constraint pk_totemguard_alerts primary key (id),
  foreign key (totemguard_player_uuid) references totemguard_player (uuid) on delete restrict on update restrict
);

create table totemguard_player (
  uuid                          varchar(40) not null,
  constraint pk_totemguard_player primary key (uuid)
);

create table totemguard_punishments (
  id                            integer not null,
  check_name                    varchar(255) not null,
  totemguard_player_uuid        varchar(40) not null,
  when_created                  timestamp not null,
  constraint pk_totemguard_punishments primary key (id),
  foreign key (totemguard_player_uuid) references totemguard_player (uuid) on delete restrict on update restrict
);

