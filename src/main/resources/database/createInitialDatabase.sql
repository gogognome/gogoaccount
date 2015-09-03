CREATE SEQUENCE domain_class_sequence START WITH 1;

CREATE TABLE account (
  id varchar2(100),
  name varchar2(1000),
  type varchar2(100),
  PRIMARY KEY (id)
);

CREATE TABLE bookkeeping (
  id int,
  description varchar2(1000),
  start_of_period date,
  currency varchar2(3),
  PRIMARY KEY (id)
);

CREATE TABLE party (
  id varchar2(100),
  name varchar2(1000),
  address varchar2(1000),
  zip_code varchar2(1000),
  city varchar2(1000),
  birth_date date,
  type varchar2(1000),
  remarks varchar2(10000),
  PRIMARY KEY (id)
);

INSERT INTO bookkeeping (id, description, start_of_period, currency) values (1, 'New bookkeeping', current_date(), 'EUR');