-- apply changes
create table totemguard_alert (
  id                            bigint auto_increment not null,
  check_name                    varchar(255) not null,
  details                       TEXT,
  totemguard_player_uuid        varchar(40) not null,
  timestamp                     datetime(6) not null,
  constraint pk_totemguard_alert primary key (id)
);

create table totemguard_player (
  uuid                          varchar(36) not null,
  constraint pk_totemguard_player primary key (uuid)
);

create table totemguard_punishment (
  id                            bigint auto_increment not null,
  check_name                    varchar(255) not null,
  totemguard_player_uuid        varchar(40) not null,
  timestamp                     datetime(6) not null,
  constraint pk_totemguard_punishment primary key (id)
);

-- foreign keys and indices
alter table totemguard_alert add constraint fk_totemguard_alert_totemguard_player_uuid foreign key (totemguard_player_uuid) references totemguard_player (uuid) on delete restrict on update restrict;

alter table totemguard_punishment add constraint fk_totemguard_punishment_totemguard_player_uuid foreign key (totemguard_player_uuid) references totemguard_player (uuid) on delete restrict on update restrict;

create index idx_alert_player_uuid on totemguard_alert (totemguard_player_uuid);
create index idx_player_uuid on totemguard_player (uuid);
create index idx_punishment_player_uuid on totemguard_punishment (totemguard_player_uuid);
