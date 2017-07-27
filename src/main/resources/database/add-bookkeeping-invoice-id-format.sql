ALTER TABLE bookkeeping ADD invoice_id_format VARCHAR2(100);

UPDATE bookkeeping SET invoice_id_format='yyyynnnnn';