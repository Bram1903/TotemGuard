-- apply changes
create table totemguard_alerts (
  id                            integer not null,
  check_name                    varchar(255),
  totemguard_player_id          integer,
  when_created                  timestamp not null,
  constraint pk_totemguard_alerts primary key (id),
  foreign key (totemguard_player_id) references totemguard_player (id) on delete restrict on update restrict
);

create table totemguard_player (
  id                            integer not null,
  uuid                          varchar(40),
  constraint pk_totemguard_player primary key (id)
);

create table totemguard_punishments (
  id                            integer not null,
  check_name                    varchar(255),
  totemguard_player_id          integer,
  when_created                  timestamp not null,
  constraint pk_totemguard_punishments primary key (id),
  foreign key (totemguard_player_id) references totemguard_player (id) on delete restrict on update restrict
);

