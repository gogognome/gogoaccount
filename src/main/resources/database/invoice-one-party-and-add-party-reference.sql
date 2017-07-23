ALTER TABLE invoice DROP CONSTRAINT fk_invoice_concerning_party_id;
ALTER TABLE invoice ALTER COLUMN paying_party_id RENAME TO party_id;
ALTER TABLE invoice DROP COLUMN concerning_party_id;
ALTER TABLE invoice ADD party_reference VARCHAR2(100) DEFAULT NULL;