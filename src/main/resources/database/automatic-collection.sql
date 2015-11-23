ALTER TABLE bookkeeping ADD organization_name VARCHAR2(1000);
ALTER TABLE bookkeeping ADD organization_address VARCHAR2(1000);
ALTER TABLE bookkeeping ADD organization_zip_code VARCHAR2(1000);
ALTER TABLE bookkeeping ADD organization_city VARCHAR2(1000);
ALTER TABLE bookkeeping ADD organization_country VARCHAR2(2);

CREATE TABLE automatic_collection_settings (
  key VARCHAR2(100),
  value VARCHAR2(1000),
  PRIMARY KEY (key)
);

CREATE TABLE party_automatic_collection_settings (
  party_id VARCHAR2(100),
  name VARCHAR2(1000),
  address VARCHAR2(1000),
  zip_code VARCHAR2(1000),
  city VARCHAR2(1000),
  country VARCHAR2(2),
  iban VARCHAR2(100),
  mandate_date DATE,
  PRIMARY KEY(party_id),
  CONSTRAINT fk_party_id_party FOREIGN KEY (party_id) REFERENCES party(id) ON DELETE CASCADE
);