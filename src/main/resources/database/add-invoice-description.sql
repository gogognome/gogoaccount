ALTER TABLE invoice ADD description VARCHAR2(1000);

UPDATE invoice SET description=(SELECT description FROM invoice_detail WHERE id IN (SELECT MIN(id) FROM invoice_detail WHERE invoice_id=invoice.id GROUP BY invoice_id));

DELETE FROM invoice_detail WHERE id IN (SELECT MIN(id) FROM invoice_detail GROUP BY invoice_id);


