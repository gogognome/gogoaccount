CREATE TABLE party_tag (
  party_id VARCHAR2(100),
  tag VARCHAR2(100),
  index int,
  PRIMARY KEY (party_id, index),
  CONSTRAINT fk_party_tag_party_id FOREIGN KEY (party_id) REFERENCES party(id) ON DELETE CASCADE
);

INSERT INTO party_tag (party_id, tag, index) SELECT id, type, 1 FROM party WHERE type is not null AND type <> '';

ALTER TABLE party DROP COLUMN type;