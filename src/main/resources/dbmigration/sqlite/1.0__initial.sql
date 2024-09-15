-- apply changes
create table alert (
  id                            integer not null,
  uuid                          varchar(40),
  check_name                    varchar(255),
  when_created                  timestamp not null,
  constraint pk_alert primary key (id)
);

create table punishment (
  id                            integer not null,
  uuid                          varchar(40),
  check_name                    varchar(255),
  when_created                  timestamp not null,
  constraint pk_punishment primary key (id)
);

