ALTER TABLE automatic_collection_settings RENAME TO direct_debit_settings;
ALTER TABLE party_automatic_collection_settings RENAME TO party_direct_debit_settings;

UPDATE settings SET key='sepaDirectDebitContractNumber' WHERE key='automaticCollectionContractNumber';

ALTER TABLE bookkeeping ALTER COLUMN enable_automatic_collection RENAME TO enable_sepa_direct_debit;
