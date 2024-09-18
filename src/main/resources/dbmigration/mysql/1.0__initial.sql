-- apply changes
create table totemguard_alerts (
  id                            bigint auto_increment not null,
  check_name                    integer,
  totemguard_player_id          bigint,
  when_created                  datetime(6) not null,
  constraint pk_totemguard_alerts primary key (id)
);

create table totemguard_player (
  id                            bigint auto_increment not null,
  uuid                          varchar(40),
  constraint pk_totemguard_player primary key (id)
);

create table totemguard_punishments (
  id                            bigint auto_increment not null,
  check_name                    integer,
  totemguard_player_id          bigint,
  when_created                  datetime(6) not null,
  constraint pk_totemguard_punishments primary key (id)
);

-- foreign keys and indices
create index ix_totemguard_alerts_totemguard_player_id on totemguard_alerts (totemguard_player_id);
alter table totemguard_alerts add constraint fk_totemguard_alerts_totemguard_player_id foreign key (totemguard_player_id) references totemguard_player (id) on delete restrict on update restrict;

create index ix_totemguard_punishments_totemguard_player_id on totemguard_punishments (totemguard_player_id);
alter table totemguard_punishments add constraint fk_totemguard_punishments_totemguard_player_id foreign key (totemguard_player_id) references totemguard_player (id) on delete restrict on update restrict;

