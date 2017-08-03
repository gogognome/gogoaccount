ALTER TABLE bookkeeping ADD party_id_format VARCHAR2(100);

UPDATE bookkeeping SET party_id_format='nnnn';