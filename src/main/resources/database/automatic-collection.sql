ALTER TABLE bookkeeping ADD organization_name VARCHAR2(1000);
ALTER TABLE bookkeeping ADD organization_address VARCHAR2(1000);
ALTER TABLE bookkeeping ADD organization_zip_code VARCHAR2(1000);
ALTER TABLE bookkeeping ADD organization_city VARCHAR2(1000);

CREATE TABLE automatic_collection_settings (
  key VARCHAR2(100),
  value VARCHAR2(1000),
  PRIMARY KEY (key)
);
