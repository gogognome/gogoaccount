CREATE SEQUENCE domain_class_sequence START WITH 1;

CREATE TABLE account (
  id varchar2(100),
  name varchar2(1000),
  type varchar2(100),
  PRIMARY KEY (id)
);

CREATE TABLE bookkeeping (
  id int,
  start_of_period date,
  currency varchar2(3),
  PRIMARY KEY (id)
);