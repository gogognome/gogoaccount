CREATE TABLE invoice_sending (
  id INT,
  invoice_id VARCHAR2(100),
  date DATE,
  type VARCHAR2(100),
  PRIMARY KEY(id),
  CONSTRAINT fk_invoice_sendings_invoice_id FOREIGN KEY (invoice_id) REFERENCES invoice(id) ON DELETE CASCADE
);
