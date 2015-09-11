CREATE SEQUENCE domain_class_sequence START WITH 1;

CREATE TABLE account (
  id VARCHAR2(100),
  name VARCHAR2(1000),
  type VARCHAR2(100),
  PRIMARY KEY (id)
);

CREATE TABLE bookkeeping (
  id INT,
  description VARCHAR2(1000),
  start_of_period DATE,
  currency VARCHAR2(3),
  PRIMARY KEY (id)
);

CREATE TABLE party (
  id VARCHAR2(100),
  name VARCHAR2(1000),
  address VARCHAR2(1000),
  zip_code VARCHAR2(1000),
  city VARCHAR2(1000),
  birth_date DATE,
  type VARCHAR2(1000),
  remarks VARCHAR2(10000),
  PRIMARY KEY (id)
);

CREATE TABLE invoice (
  id VARCHAR2(100),
  concerning_party_id VARCHAR2(100),
  paying_party_id VARCHAR2(100),
  amount_to_be_paid VARCHAR2(1000),
  issue_date DATE,
  PRIMARY KEY (id),
  CONSTRAINT fk_invoice_concerning_party_id FOREIGN KEY (concerning_party_id) REFERENCES party(id),
  CONSTRAINT fk_invoice_paying_party_id FOREIGN KEY (paying_party_id) REFERENCES party(id)
);

CREATE TABLE invoice_detail (
  id VARCHAR2(100),
  invoice_id VARCHAR2(100),
  description VARCHAR2(1000),
  amount VARCHAR2(1000),
  PRIMARY KEY (id),
  CONSTRAINT fk_invoice_detail_invoice_id FOREIGN KEY (invoice_id) REFERENCES invoice(id) ON DELETE CASCADE
);

CREATE TABLE payment (
  id VARCHAR2(100),
  invoice_id VARCHAR2(100),
  date DATE,
  amount VARCHAR2(1000),
  description VARCHAR2(1000),
  PRIMARY KEY (id),
  CONSTRAINT fk_payment_invoice_id FOREIGN KEY (invoice_id) REFERENCES invoice(id) ON DELETE CASCADE
);

INSERT INTO bookkeeping (id, description, start_of_period, currency) values (1, 'New bookkeeping', current_date(), 'EUR');