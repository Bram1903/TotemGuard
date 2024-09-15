-- apply changes
create table alert (
  id                            bigint auto_increment not null,
  uuid                          varchar(40),
  check_name                    varchar(255),
  when_created                  datetime(6) not null,
  constraint pk_alert primary key (id)
);

create table punishment (
  id                            bigint auto_increment not null,
  uuid                          varchar(40),
  check_name                    varchar(255),
  when_created                  datetime(6) not null,
  constraint pk_punishment primary key (id)
);

